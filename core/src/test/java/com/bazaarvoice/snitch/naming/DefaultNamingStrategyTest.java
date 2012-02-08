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
package com.bazaarvoice.snitch.naming;

import com.bazaarvoice.snitch.Monitored;
import com.google.common.base.Throwables;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class DefaultNamingStrategyTest {
    private final DefaultNamingStrategy _strategy = DefaultNamingStrategy.INSTANCE;

    @Test
    public void testNonAnnotatedField() {
        Field field = getFieldNamed("nonAnnotatedField");
        Annotation annotation = null;

        assertEquals(field.getName(), _strategy.getName(field, annotation));
    }

    @Test
    public void testAnnotatedFieldWithNoName() {
        Field field = getFieldNamed("annotatedFieldWithNoName");
        Annotation annotation = field.getAnnotation(Monitored.class);

        assertEquals(field.getName(), _strategy.getName(field, annotation));
    }

    @Test
    public void testAnnotatedFieldWithName() {
        Field field = getFieldNamed("annotatedFieldWithName");
        Annotation annotation = field.getAnnotation(Monitored.class);

        assertEquals("custom-field-name", _strategy.getName(field, annotation));
    }

    @Test
    public void testAnnotatedFieldWithCustomAnnotation() {
        Field field = getFieldNamed("annotatedFieldWithCustomAnnotation");
        Annotation annotation = field.getAnnotation(Foo.class);

        assertEquals(field.getName(), _strategy.getName(field, annotation));
    }

    @Test
    public void testNonAnnotatedMethod() {
        Method method = getMethodNamed("nonAnnotatedMethod");
        Annotation annotation = null;
        
        assertEquals(method.getName(), _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedMethodWithNoName() {
        Method method = getMethodNamed("annotatedMethodWithNoName");
        Annotation annotation = method.getAnnotation(Monitored.class);

        assertEquals(method.getName(), _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedMethodWithName() {
        Method method = getMethodNamed("annotatedMethodWithName");
        Annotation annotation = method.getAnnotation(Monitored.class);

        assertEquals("custom-method-name", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedMethodWithCustomAnnotation() {
        Method method = getMethodNamed("annotatedMethodWithCustomAnnotation");
        Annotation annotation = method.getAnnotation(Foo.class);

        assertEquals(method.getName(), _strategy.getName(method, annotation));
    }

    @Test
    public void testNonAnnotatedBooleanGetter() {
        Method method = getMethodNamed("isNonAnnotatedGetter");
        Annotation annotation = null;

        assertEquals("nonAnnotatedGetter", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedBooleanGetterWithNoName() {
        Method method = getMethodNamed("isAnnotatedGetterWithNoName");
        Annotation annotation = method.getAnnotation(Monitored.class);

        assertEquals("annotatedGetterWithNoName", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedBooleanGetterWithName() {
        Method method = getMethodNamed("isAnnotatedGetterWithName");
        Annotation annotation = method.getAnnotation(Monitored.class);

        assertEquals("custom-boolean-getter-name", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedBooleanGetterWithCustomAnnotation() {
        Method method = getMethodNamed("isAnnotatedGetterWithCustomAnnotation");
        Annotation annotation = method.getAnnotation(Foo.class);

        assertEquals("annotatedGetterWithCustomAnnotation", _strategy.getName(method, annotation));
    }

    @Test
    public void testNonAnnotatedGetter() {
        Method method = getMethodNamed("getNonAnnotatedGetter");
        Annotation annotation = null;

        assertEquals("nonAnnotatedGetter", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedGetterWithNoName() {
        Method method = getMethodNamed("getAnnotatedGetterWithNoName");
        Annotation annotation = method.getAnnotation(Monitored.class);

        assertEquals("annotatedGetterWithNoName", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedGetterWithName() {
        Method method = getMethodNamed("getAnnotatedGetterWithName");
        Annotation annotation = method.getAnnotation(Monitored.class);

        assertEquals("custom-getter-name", _strategy.getName(method, annotation));
    }

    @Test
    public void testAnnotatedGetterWithCustomAnnotation() {
        Method method = getMethodNamed("getAnnotatedGetterWithCustomAnnotation");
        Annotation annotation = method.getAnnotation(Foo.class);

        assertEquals("annotatedGetterWithCustomAnnotation", _strategy.getName(method, annotation));
    }

    private static Field getFieldNamed(String name) {
        try {
            return TestClass.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw Throwables.propagate(e);
        }
    }

    private static Method getMethodNamed(String name) {
        try {
            return TestClass.class.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    public static @interface Foo {}

    private static final class TestClass {
        // Fields
        private final int nonAnnotatedField = 0;
        @Monitored public final int annotatedFieldWithNoName = 1;
        @Monitored("custom-field-name") public final int annotatedFieldWithName = 2;
        @Foo public final int annotatedFieldWithCustomAnnotation = 3;
        
        // Normal methods
        private int nonAnnotatedMethod() { return 4; }
        @Monitored private int annotatedMethodWithNoName() { return 5; }
        @Monitored("custom-method-name") private int annotatedMethodWithName() { return 6; }
        @Foo private int annotatedMethodWithCustomAnnotation() { return 7; }
        
        // Boolean getter methods
        private boolean isNonAnnotatedGetter() { return true; }
        @Monitored private boolean isAnnotatedGetterWithNoName() { return false; }
        @Monitored("custom-boolean-getter-name") private boolean isAnnotatedGetterWithName() { return true; }
        @Foo private boolean isAnnotatedGetterWithCustomAnnotation() { return false; }
        
        // Non-boolean getter methods
        private int getNonAnnotatedGetter() { return 8; }
        @Monitored private int getAnnotatedGetterWithNoName() { return 9; }
        @Monitored("custom-getter-name") private int getAnnotatedGetterWithName() { return 10; }
        @Foo private int getAnnotatedGetterWithCustomAnnotation() { return 11; }
    }
}
