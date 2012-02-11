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
import com.bazaarvoice.snitch.naming.DefaultNamingStrategy;
import com.bazaarvoice.snitch.naming.NamingStrategy;
import com.bazaarvoice.snitch.scanner.AnnotationScanner;
import com.bazaarvoice.snitch.util.ClassDetector;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;

import static com.bazaarvoice.snitch.scanner.AnnotationScanner.FieldEntry;
import static com.bazaarvoice.snitch.scanner.AnnotationScanner.MethodEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableRegistryTest {
    private final AnnotationScanner _scanner = mock(AnnotationScanner.class);
    private final NamingStrategy _namingStrategy = DefaultNamingStrategy.INSTANCE;
    private final ClassDetector _detector = mock(ClassDetector.class);
    private final VariableRegistry _registry = new VariableRegistry(Foo.class, _scanner, _namingStrategy, _detector);

    @Test
    public void testFieldInUnloadedClass() {
        List<FieldEntry> fields = mockFields(CLASS_NAME, FIELD_NAME, STATIC_FIELD_NAME);
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(false);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.isEmpty(variables));
    }

    @Test
    public void testMethodInUnloadedClass() {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_NAME, STATIC_METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(false);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.isEmpty(variables));
    }

    @Test
    public void testStaticFieldInLoadedClass() throws NoSuchFieldException {
        List<FieldEntry> fields = mockFields(CLASS_NAME, STATIC_FIELD_NAME);
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof FieldVariable);

        FieldVariable variable = (FieldVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestClass.class.getField(STATIC_FIELD_NAME), variable.getField());
    }

    @Test
    public void testStaticMethodInLoadedClass() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, STATIC_METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof MethodVariable);

        MethodVariable variable = (MethodVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestClass.class.getMethod(STATIC_METHOD_NAME), variable.getMethod());
    }

    @Test
    public void testStaticMethodWithArgumentInLoadedClass() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, STATIC_METHOD_WITH_ARGUMENTS_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.isEmpty(variables));
    }

    @Test
    public void testStaticMethodWithVoidReturnTypeInLoadedClass() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, STATIC_METHOD_WITH_VOID_RETURN_TYPE);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.isEmpty(variables));
    }

    @Test
    public void testFieldInRegisteredInstance() throws NoSuchFieldException {
        List<FieldEntry> fields = mockFields(CLASS_NAME, FIELD_NAME);
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        TestClass instance = new TestClass();
        _registry.registerInstance(instance);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof FieldVariable);

        FieldVariable variable = (FieldVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestClass.class.getField(FIELD_NAME), variable.getField());
    }

    @Test
    public void testInheritedFieldInRegisteredInstance() throws NoSuchFieldException {
        List<FieldEntry> fields = mockFields(CLASS_NAME, FIELD_NAME);  // System only sees fields in parent class
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when(_detector.isClassLoaded(SUBCLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);
        when((Class) _detector.getLoadedClass(SUBCLASS_NAME)).thenReturn(TestSubclass.class);

        TestSubclass instance = new TestSubclass();
        _registry.registerInstance(instance);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof FieldVariable);

        FieldVariable variable = (FieldVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestClass.class.getField(FIELD_NAME), variable.getField());
    }

    @Test
    public void testMethodInRegisteredInstance() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        assertTrue(Iterables.isEmpty(_registry.getVariables()));

        TestClass instance = new TestClass();
        _registry.registerInstance(instance);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof MethodVariable);

        MethodVariable variable = (MethodVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestClass.class.getMethod(METHOD_NAME), variable.getMethod());
    }

    @Test
    public void testMethodWithArgumentInRegisteredInstance() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_WITH_ARGUMENTS_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        TestClass instance = new TestClass();
        _registry.registerInstance(instance);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.isEmpty(variables));
    }

    @Test
    public void testMethodWithVoidReturnTypeInLoadedClass() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_WITH_VOID_RETURN_TYPE);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.isEmpty(variables));
    }

    @Test
    public void testInheritedMethodInRegisteredInstance() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when(_detector.isClassLoaded(SUBCLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);
        when((Class) _detector.getLoadedClass(SUBCLASS_NAME)).thenReturn(TestSubclass.class);

        assertTrue(Iterables.isEmpty(_registry.getVariables()));

        TestSubclass instance = new TestSubclass();
        _registry.registerInstance(instance);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof MethodVariable);

        MethodVariable variable = (MethodVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestClass.class.getMethod(METHOD_NAME), variable.getMethod());
    }

    @Test
    public void testInheritedMethodCalledInRegisteredInstance() throws NoSuchMethodException {
        List<MethodEntry> methods1 = mockMethods(CLASS_NAME, OVERRIDDEN_METHOD_NAME);
        List<MethodEntry> methods2 = mockMethods(SUBCLASS_NAME, OVERRIDDEN_METHOD_NAME);
        List<MethodEntry> methods = Lists.newArrayList(Iterables.concat(methods1, methods2));
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when(_detector.isClassLoaded(SUBCLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);
        when((Class) _detector.getLoadedClass(SUBCLASS_NAME)).thenReturn(TestSubclass.class);

        TestSubclass instance = new TestSubclass();
        _registry.registerInstance(instance);

        Iterable<Variable> variables = _registry.getVariables();
        assertTrue(Iterables.getOnlyElement(variables) instanceof MethodVariable);

        MethodVariable variable = (MethodVariable) Iterables.getOnlyElement(variables);
        assertEquals(TestSubclass.class.getMethod(OVERRIDDEN_METHOD_NAME), variable.getMethod());
        assertEquals(1, variable.getValue());
    }

    @Test
    public void testAddInstanceVariableWhileIterating() throws NoSuchFieldException {
        List<FieldEntry> fields = mockFields(CLASS_NAME, STATIC_FIELD_NAME, FIELD_NAME);
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        // Should initially just contain the static field
        Iterable<Variable> variables = _registry.getVariables();
        Iterator<Variable> iterator = variables.iterator();

        // Register an instance so that it finds another variable
        TestClass instance = new TestClass();
        _registry.registerInstance(instance);

        // Our variables should now contain 2 entries
        assertEquals(2, Iterables.size(variables));

        // And our iterator that was created before the 2nd variable was created should be able to see both as well
        assertEquals(2, Iterators.size(iterator));
    }

    private static List<FieldEntry> mockFields(String className, String... fieldNames) {
        List<FieldEntry> entries = Lists.newArrayList();
        for (String fieldName : fieldNames) {
            FieldEntry entry = mock(FieldEntry.class);
            when(entry.getClassName()).thenReturn(className);
            when(entry.getFieldName()).thenReturn(fieldName);
            entries.add(entry);
        }
        return entries;
    }

    private static List<MethodEntry> mockMethods(String className, String... methodNames) {
        List<MethodEntry> entries = Lists.newArrayList();
        for (String methodName : methodNames) {
            MethodEntry entry = mock(MethodEntry.class);
            when(entry.getClassName()).thenReturn(className);
            when(entry.getMethodName()).thenReturn(methodName);
            entries.add(entry);
        }
        return entries;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    public @interface Foo {}

    /////////////////////////////////////////////////////////////////////////////////////
    // Helper names and class.
    // These names should all be compatible with the fields and methods in the test class
    /////////////////////////////////////////////////////////////////////////////////////

    private static final String CLASS_NAME = TestClass.class.getName();
    private static final String SUBCLASS_NAME = TestSubclass.class.getName();
    private static final String STATIC_FIELD_NAME = "staticField";
    private static final String STATIC_METHOD_NAME = "staticMethod";
    private static final String STATIC_METHOD_WITH_ARGUMENTS_NAME = "staticMethodWithArgs";
    private static final String STATIC_METHOD_WITH_VOID_RETURN_TYPE = "staticMethodWithVoidReturn";
    private static final String FIELD_NAME = "field";
    private static final String METHOD_NAME = "method";
    private static final String METHOD_WITH_ARGUMENTS_NAME = "methodWithArgs";
    private static final String METHOD_WITH_VOID_RETURN_TYPE = "methodWithVoidReturn";
    private static final String OVERRIDDEN_METHOD_NAME = "methodToOverride";

    @SuppressWarnings("unused")
    private static class TestClass {
        @Foo public static int staticField;
        @Foo public static int staticMethod() { return 0; }
        @Foo public static int staticMethodWithArgs(int i) { return i; }
        @Foo public static void staticMethodWithVoidReturn() {}
        @Foo public int field;
        @Foo public int method() { return 0; }
        @Foo public int methodWithArgs(int i) { return i; }
        @Foo public int methodToOverride() { return 0; }
        @Foo public static void methodWithVoidReturn() {}
    }

    private static class TestSubclass extends TestClass {
        @Foo @Override public int methodToOverride() { return 1; }
    }
}
