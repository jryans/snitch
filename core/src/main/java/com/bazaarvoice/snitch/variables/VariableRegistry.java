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

import com.bazaarvoice.snitch.scanner.AnnotationScanner;
import com.bazaarvoice.snitch.util.ClassDetector;
import com.bazaarvoice.snitch.naming.NamingStrategy;
import com.bazaarvoice.snitch.util.ReflectionClassDetector;
import com.bazaarvoice.snitch.Variable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.bazaarvoice.snitch.scanner.ClassPathAnnotationScanner.FieldEntry;
import static com.bazaarvoice.snitch.scanner.ClassPathAnnotationScanner.MethodEntry;

// TODO: Javadoc for class
// TODO: Convert to use FinalizableReferences for instances and classes
// TODO: Create an error reporter that can switch between logging and throwing exceptions (dev mode)
// TODO: Don't check for classes having been loaded every time, have some sort of backoff
public class VariableRegistry {
    /** The annotation class to find.  It is assumed that this annotation can't be found on classes. */
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

    /** The live variables in the JVM. */
    private final List<Variable> _variables = Lists.newArrayList();

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
    public List<Variable> getVariables() {
        if (!_alreadyScanned) {
            scanClassPath();
        }

        checkForRecentlyLoadedClasses();

        return _variables;
    }

    /** Register an instance of a class that may have non-static variables/methods annotated. */
    public void registerInstance(Object instance) {
        if (instance == null) return;

        if (!_alreadyScanned) {
            scanClassPath();
        }

        checkForRecentlyLoadedClasses();
        
        Class<?> cls = instance.getClass();
        handleRegisteredInstance(cls, instance);
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
                _variables.add(variable);
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
                continue;  // TODO: Log error
            }

            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                MethodVariable variable = new MethodVariable(cls, getName(method), method);
                _variables.add(variable);
            } else {
                MethodHandle handle = new MethodHandle(method);
                _unboundMethodHandles.put(className, handle);
            }
        }
    }

    private void handleRegisteredInstance(Class<?> cls, Object instance) {
        while (cls != null) {
            String className = cls.getName();

            Collection<FieldHandle> fieldHandles = _unboundFieldHandles.get(className);
            handleRegisteredInstanceFields(cls, instance, fieldHandles);

            Collection<MethodHandle> methodHandles = _unboundMethodHandles.get(className);
            handleRegisteredInstanceMethods(cls, instance, methodHandles);

            cls = cls.getSuperclass();
        }
    }

    private void handleRegisteredInstanceFields(Class<?> cls, Object instance, Collection<FieldHandle> handles) {
        for (FieldHandle handle : handles) {
            Field field = handle.getField();
            FieldVariable variable = new FieldVariable(cls, getName(field), instance, field);
            
            _variables.add(variable);
        }
    }

    private void handleRegisteredInstanceMethods(Class<?> cls, Object instance, Collection<MethodHandle> handles) {
        for (MethodHandle handle : handles) {
            Method method = handle.getMethod();
            MethodVariable variable = new MethodVariable(cls, getName(method), instance, method);
            
            _variables.add(variable);
        }
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
