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
import com.bazaarvoice.snitch.naming.DefaultNamingStrategy;
import com.bazaarvoice.snitch.naming.NamingStrategy;
import com.bazaarvoice.snitch.Variable;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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

        List<Variable> variables = _registry.getVariables();
        assertTrue(variables.isEmpty());
    }

    @Test
    public void testMethodInUnloadedClass() {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_NAME, STATIC_METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(false);

        List<Variable> variables = _registry.getVariables();
        assertTrue(variables.isEmpty());
    }

    @Test
    public void testStaticFieldInLoadedClass() throws NoSuchFieldException {
        List<FieldEntry> fields = mockFields(CLASS_NAME, STATIC_FIELD_NAME);
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        List<Variable> variables = _registry.getVariables();
        assertEquals(1, variables.size());
        assertTrue(variables.get(0) instanceof FieldVariable);

        FieldVariable variable = (FieldVariable) variables.get(0);
        assertEquals(TestClass.class.getField(STATIC_FIELD_NAME), variable.getField());
    }

    @Test
    public void testStaticMethodInLoadedClass() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, STATIC_METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(Lists.newArrayList(methods));
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        List<Variable> variables = _registry.getVariables();
        assertEquals(1, variables.size());
        assertTrue(variables.get(0) instanceof MethodVariable);

        MethodVariable variable = (MethodVariable) variables.get(0);
        assertEquals(TestClass.class.getMethod(STATIC_METHOD_NAME), variable.getMethod());
    }
    
    @Test
    public void testFieldInRegisteredInstance() throws NoSuchFieldException {
        List<FieldEntry> fields = mockFields(CLASS_NAME, FIELD_NAME);
        when(_scanner.getFieldsAnnotatedWith(Foo.class)).thenReturn(fields);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        TestClass instance = new TestClass();
        _registry.registerInstance(instance);
        
        List<Variable> variables = _registry.getVariables();
        assertEquals(1, variables.size());
        assertTrue(variables.get(0) instanceof FieldVariable);
        
        FieldVariable variable = (FieldVariable) variables.get(0);
        assertEquals(TestClass.class.getField(FIELD_NAME), variable.getField());
    }

    @Test
    public void testMethodInRegisteredInstance() throws NoSuchMethodException {
        List<MethodEntry> methods = mockMethods(CLASS_NAME, METHOD_NAME);
        when(_scanner.getMethodsAnnotatedWith(Foo.class)).thenReturn(methods);
        when(_detector.isClassLoaded(CLASS_NAME)).thenReturn(true);
        when((Class) _detector.getLoadedClass(CLASS_NAME)).thenReturn(TestClass.class);

        assertTrue(_registry.getVariables().isEmpty());

        TestClass instance = new TestClass();
        _registry.registerInstance(instance);

        List<Variable> variables = _registry.getVariables();
        assertEquals(1, variables.size());
        assertTrue(variables.get(0) instanceof MethodVariable);

        MethodVariable variable = (MethodVariable) variables.get(0);
        assertEquals(TestClass.class.getMethod(METHOD_NAME), variable.getMethod());
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
    private static final String STATIC_FIELD_NAME = "staticField";
    private static final String STATIC_METHOD_NAME = "staticMethod";
    private static final String FIELD_NAME = "field";
    private static final String METHOD_NAME = "method";

    @SuppressWarnings("unused")
    private static final class TestClass {
        @Foo public static int staticField;
        @Foo public static int staticMethod() { return 0; }
        @Foo public int field;
        @Foo public int method() { return 0; }
    }
}
