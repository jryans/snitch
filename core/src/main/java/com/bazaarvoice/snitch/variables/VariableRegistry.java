/*
 * Copyright 2012 Bazaarvoice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bazaarvoice.snitch.variables;

import com.bazaarvoice.snitch.Variable;
import com.bazaarvoice.snitch.naming.NamingStrategy;
import com.bazaarvoice.snitch.scanner.AnnotationScanner;
import com.bazaarvoice.snitch.util.ClassDetector;
import com.bazaarvoice.snitch.util.ReflectionClassDetector;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.bazaarvoice.snitch.scanner.ClassPathAnnotationScanner.FieldEntry;
import static com.bazaarvoice.snitch.scanner.ClassPathAnnotationScanner.MethodEntry;

// TODO: Javadoc for class
// TODO: Create an error reporter that can switch between logging and throwing exceptions (dev mode)
// TODO: Don't check for classes having been loaded every time, have some sort of backoff
// TODO: Make sure method has non-void return type
public class VariableRegistry {
    /** The annotation class to find. */
    private final Class<? extends Annotation> _annotationClass;

    /** The scanner that finds annotated methods and fields. */
    private final AnnotationScanner _scanner;

    /**
     * Naming strategy used for giving variables readable names.
     * <p/>
     * NOTE: We store an unbound instance of it because we don't know at compile time which subclass of Annotation
     * we'll be working with.  The framework makes the guarantee that a naming strategy will be paired with
     * a compatible annotation class, thus the unchecked calls that might happen should actually never happen.  If this
     * guarantee is violated then we may end up throwing exceptions while trying to name fields and methods.
     */
    private final NamingStrategy _namingStrategy;

    /** Helper that knows whether or not a class has already been loaded in the JVM. */
    private final ClassDetector _classDetector;

    /** Whether or not we've already scanned for annotations in the class path. */
    private boolean _alreadyScanned = false;

    /** The annotated field entries that were found when the class path was scanned.  Indexed by class name. */
    private final Multimap<String, FieldEntry> _unloadedFieldEntries = HashMultimap.create();

    /** The annotated method entries that were found when the class path was scanned.  Indexed by class name. */
    private final Multimap<String, MethodEntry> _unloadedMethodEntries = HashMultimap.create();

    /** The non-static field handles that were found when the class path was scanned.  Indexed by class name. */
    private final Multimap<String, FieldHandle> _unboundFieldHandles = HashMultimap.create();

    /** The non-static method handles that were found when the class path was scanned.  Indexed by class name. */
    private final Multimap<String, MethodHandle> _unboundMethodHandles = HashMultimap.create();

    /**
     * The variables in the JVM that are static.  This storage is separate from the instance variable storage for two
     * reasons.  First, concurrent maps typically don't permit null keys, and since these are static variables they
     * don't have an instance backing them and thus the logical choice would be null for the instance (that's how all
     * of the reflection APIs work).  Secondly, the write pattern to this set of variables is quite different from the
     * write pattern for instance backed variables.  Instance backed variables are only ever discovered when the
     * instance is registered and then never again.  So the storage for them can be optimized around very infrequent
     * writes.  On the other hand, the static variables storage is written to whenever a new class is loaded that
     * contains an annotation.  This doesn't happen very often, but will happen multiple times during the lifetime of
     * this storage so a CopyOnWriteArrayList isn't a very good choice for the static variables since writes are very
     * expensive.
     */
    private final Collection<Variable> _staticVariables = new ConcurrentLinkedQueue<Variable>();

    /** The variables in the JVM indexed by instance that caused the variable to be created. */
    private final Multimap<Object, Variable> _instanceVariables = Multimaps.newMultimap(
            // The underlying map to use for storage.  Use weak keys so that we don't maintain any strong references
            // to instances that are given to us.  They should always be able to be reclaimed by the garbage collector.
            new MapMaker().weakKeys().<Object, Collection<Variable>>makeMap(),

            // The collection factory, use a CopyOnWriteArrayList since writes happen very infrequently.
            new Supplier<Collection<Variable>>() {
                @Override
                public Collection<Variable> get() {
                    return new CopyOnWriteArrayList<Variable>();
                }
            }
    );

    public VariableRegistry(Class<? extends Annotation> annotationClass, AnnotationScanner scanner,
                            NamingStrategy<? extends Annotation> namingStrategy) {
        this(annotationClass, scanner, namingStrategy, new ReflectionClassDetector());
    }

    @VisibleForTesting
    VariableRegistry(Class<? extends Annotation> annotationClass, AnnotationScanner scanner,
                     NamingStrategy namingStrategy, ClassDetector detector) {
        _annotationClass = annotationClass;
        _scanner = scanner;
        _namingStrategy = namingStrategy;
        _classDetector = detector;

        _scanner.addAnnotationClass(_annotationClass);
    }

    /** Return the set of known variables in the system. */
    public Iterable<Variable> getVariables() {
        if (!_alreadyScanned) {
            scanClassPath();
        }

        checkForRecentlyLoadedClasses();

        // We need to merge _staticVariables and _instanceVariables together here.  We know that they're both
        // implemented using thread-safe collections, so we can just concatenate them together and be confident that
        // any consumer will never experience any inconsistencies while iterating.
        return Iterables.unmodifiableIterable(Iterables.concat(_staticVariables, _instanceVariables.values()));
    }

    /** Register an instance of a class that may have non-static variables/methods annotated. */
    public synchronized void registerInstance(final Object instance) {
        if (instance == null) return;

        if (!_alreadyScanned) {
            scanClassPath();
        }

        checkForRecentlyLoadedClasses();

        // Wrap the instance into a supplier that can create the weak reference for it.  This lets us defer creation of
        // the weak reference until we know for sure that we need it.  We use a memoized supplier as well so we know for
        // sure that we'll only ever create one weak reference for this instance and use it in all variables.
        Supplier<WeakReference<Object>> supplier = Suppliers.memoize(new Supplier<WeakReference<Object>>() {
            @Override
            public WeakReference<Object> get() {
                return new WeakReference<Object>(instance);
            }
        });

        Collection<Variable> instanceVariables = createInstanceVariables(instance.getClass(), supplier);
        _instanceVariables.putAll(instance, instanceVariables);
    }

    private synchronized void scanClassPath() {
        if (_alreadyScanned) {
            return;
        }

        List<MethodEntry> methodEntries = _scanner.getMethodsAnnotatedWith(_annotationClass);
        for (MethodEntry entry : methodEntries) {
            _unloadedMethodEntries.put(entry.getClassName(), entry);
        }

        List<FieldEntry> fieldEntries = _scanner.getFieldsAnnotatedWith(_annotationClass);
        for (FieldEntry entry : fieldEntries) {
            _unloadedFieldEntries.put(entry.getClassName(), entry);
        }

        _alreadyScanned = true;
    }

    private synchronized void checkForRecentlyLoadedClasses() {
        // When we scan the class path several things can happen:
        //
        //   1) The class that the elements we found belong to has already been loaded by the JVM.  In this case all
        //   static elements are immediately converted into variables.  The non-static elements are remembered so that
        //   later when an instance is registered variables can be created.
        //
        //   2) The class that the elements we found belong to hasn't yet been loaded by the JVM.  In this case the
        //   entries need to be remembered for later when the class has been loaded by the JVM.
        Set<String> classNames = union(_unloadedMethodEntries.keySet(), _unloadedFieldEntries.keySet());
        for (String className : classNames) {
            if (_classDetector.isClassLoaded(className)) {
                Class<?> cls = _classDetector.getLoadedClass(className);
                if (cls == null) {
                    continue;  // TODO: Log error
                }

                handleLoadedClass(className, cls);
            }
        }
    }

    private void handleLoadedClass(String className, Class<?> cls) {
        Collection<FieldEntry> fieldEntries = _unloadedFieldEntries.removeAll(className);
        handleLoadedClassFields(className, cls, fieldEntries);

        Collection<MethodEntry> methodEntries = _unloadedMethodEntries.removeAll(className);
        handleLoadedClassMethods(className, cls, methodEntries);
    }

    private void handleLoadedClassFields(String className, Class<?> cls, Collection<FieldEntry> entries) {
        for (FieldEntry entry : entries) {
            Field field = getAnnotatedField(cls, _annotationClass, entry.getFieldName());
            if (field == null) {
                continue;  // TODO: Log error
            }

            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                FieldVariable variable = new FieldVariable(cls, getName(field), field);
                _staticVariables.add(variable);
            } else {
                FieldHandle handle = new FieldHandle(field);
                _unboundFieldHandles.put(className, handle);
            }
        }
    }

    private void handleLoadedClassMethods(String className, Class<?> cls, Collection<MethodEntry> entries) {
        for (MethodEntry entry : entries) {
            Method method = getAnnotatedMethod(cls, _annotationClass, entry.getMethodName());
            if (method == null) {
                continue;  // This can happen if a method has arguments, getAnnotatedMethod won't load it.
            }

            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                MethodVariable variable = new MethodVariable(cls, getName(method), method);
                _staticVariables.add(variable);
            } else {
                MethodHandle handle = new MethodHandle(method);
                _unboundMethodHandles.put(className, handle);
            }
        }
    }

    private Collection<Variable> createInstanceVariables(Class<?> cls,
                                                         Supplier<WeakReference<Object>> referenceSupplier) {
        // We need to make sure we only resolve each method name one time.  We're making the assumption here that
        // all methods have no arguments, so the only relevant component is the name of the method.  Doing this
        // check is important because it's possible that a base class will define a method that is overridden by
        // a subclass.  Java would normally only ever call the subclass version so we should do the same.  We're
        // starting with the subclass, so the first time we see a given method name should be the only time we
        // process it.
        Set<String> seenMethodNames = Sets.newHashSet();

        Collection<Variable> variables = Lists.newArrayList();
        while (cls != null) {
            String className = cls.getName();

            Collection<FieldHandle> fieldHandles = _unboundFieldHandles.get(className);
            for (FieldHandle handle : fieldHandles) {
                Field field = handle.getField();
                FieldVariable variable = new FieldVariable(cls, getName(field), referenceSupplier.get(), field);

                variables.add(variable);
            }

            Collection<MethodHandle> methodHandles = _unboundMethodHandles.get(className);
            for (MethodHandle handle : methodHandles) {
                Method method = handle.getMethod();

                if (seenMethodNames.add(method.getName())) {
                  MethodVariable variable = new MethodVariable(cls, getName(method), referenceSupplier.get(), method);
                  variables.add(variable);
                }
            }

            cls = cls.getSuperclass();
        }

        return variables;
    }

    @SuppressWarnings("unchecked")
    private String getName(Field field) {
        Annotation annotation = field.getAnnotation(_annotationClass);
        return _namingStrategy.getName(field, annotation);
    }

    @SuppressWarnings("unchecked")
    private String getName(Method method) {
        Annotation annotation = method.getAnnotation(_annotationClass);
        return _namingStrategy.getName(method, annotation);
    }

    /** Create a new set that's the union of two sets. */
    private static <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> union = Sets.newHashSet();
        union.addAll(a);
        union.addAll(b);
        return union;
    }

    private static Method getAnnotatedMethod(Class<?> cls, Class<? extends Annotation> annotationClass, String name) {
        Method method;
        try {
            method = cls.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }

        // Make sure the method has the annotation on it
        if (!method.isAnnotationPresent(annotationClass)) {
            return null;
        }

        return method;
    }

    private static Field getAnnotatedField(Class<?> cls, Class<? extends Annotation> annotationClass, String name) {
        Field field;
        try {
            field = cls.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }

        // Make sure the field has the annotation on it
        if (!field.isAnnotationPresent(annotationClass)) {
            return null;
        }

        return field;
    }

    private static final class FieldHandle {
        private final Field _field;

        public FieldHandle(Field field) {
            _field = field;
        }

        public Field getField() {
            return _field;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("field", _field)
                    .toString();
        }
    }

    private static final class MethodHandle {
        private final Method _method;

        public MethodHandle(Method method) {
            _method = method;
        }

        public Method getMethod() {
            return _method;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("method", _method)
                    .toString();
        }
    }
}
