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
package com.bazaarvoice.snitch;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import static com.bazaarvoice.snitch.ClassPathAnnotationScanner.ClassEntry;
import static com.bazaarvoice.snitch.ClassPathAnnotationScanner.FieldEntry;
import static com.bazaarvoice.snitch.ClassPathAnnotationScanner.MethodEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClassPathAnnotationScannerTest {
    private static final String PACKAGE_NAME = ClassPathAnnotationScannerTest.class.getPackage().getName();
    private final ClassPathAnnotationScanner _scanner;

    public ClassPathAnnotationScannerTest() {
        _scanner = new ClassPathAnnotationScanner(PACKAGE_NAME);
        _scanner.addAnnotationClass(Foo.class);
    }

    @Test
    public void testAnnotatedClasses() {
        Collection<ClassEntry> entries = _scanner.getClassesAnnotatedWith(Foo.class);
        assertContainsType(entries, TestClass.class);
    }

    @Test
    public void testAnnotatedInnerClasses() {
        Collection<ClassEntry> entries = _scanner.getClassesAnnotatedWith(Foo.class);
        assertContainsType(entries, TestClass.PublicFoo.class);
        assertContainsType(entries, TestClass.PackageFoo.class);
        assertContainsType(entries, TestClass.ProtectedFoo.class);
        assertContainsType(entries, TestClass.PrivateFoo.class);
        assertNotContainsType(entries, TestClass.PublicBar.class);
        assertNotContainsType(entries, TestClass.PackageBar.class);
        assertNotContainsType(entries, TestClass.ProtectedBar.class);
        assertNotContainsType(entries, TestClass.PrivateBar.class);
    }

    @Test
    public void testAnnotatedStaticInnerClasses() {
        Collection<ClassEntry> entries = _scanner.getClassesAnnotatedWith(Foo.class);
        assertContainsType(entries, TestClass.PublicStaticFoo.class);
        assertContainsType(entries, TestClass.PackageStaticFoo.class);
        assertContainsType(entries, TestClass.ProtectedStaticFoo.class);
        assertContainsType(entries, TestClass.PrivateStaticFoo.class);
        assertNotContainsType(entries, TestClass.PublicStaticBar.class);
        assertNotContainsType(entries, TestClass.PackageStaticBar.class);
        assertNotContainsType(entries, TestClass.ProtectedStaticBar.class);
        assertNotContainsType(entries, TestClass.PrivateStaticBar.class);
    }

    @Test
    public void testAnnotatedMethods() {
        Collection<MethodEntry> entries = _scanner.getMethodsAnnotatedWith(Foo.class);
        assertContainsMethod(entries, TestClass.class, "publicFoo");
        assertContainsMethod(entries, TestClass.class, "packageFoo");
        assertContainsMethod(entries, TestClass.class, "protectedFoo");
        assertContainsMethod(entries, TestClass.class, "privateFoo");
        assertNotContainsMethod(entries, TestClass.class, "publicBar");
        assertNotContainsMethod(entries, TestClass.class, "packageBar");
        assertNotContainsMethod(entries, TestClass.class, "protectedBar");
        assertNotContainsMethod(entries, TestClass.class, "privateBar");
    }

    @Test
    public void testAnnotatedStaticMethods() {
        Collection<MethodEntry> entries = _scanner.getMethodsAnnotatedWith(Foo.class);
        assertContainsMethod(entries, TestClass.class, "publicStaticFoo");
        assertContainsMethod(entries, TestClass.class, "packageStaticFoo");
        assertContainsMethod(entries, TestClass.class, "protectedStaticFoo");
        assertContainsMethod(entries, TestClass.class, "privateStaticFoo");
        assertNotContainsMethod(entries, TestClass.class, "publicStaticBar");
        assertNotContainsMethod(entries, TestClass.class, "packageStaticBar");
        assertNotContainsMethod(entries, TestClass.class, "protectedStaticBar");
        assertNotContainsMethod(entries, TestClass.class, "privateStaticBar");
    }

    @Test
    public void testAnnotatedFields() {
        Collection<FieldEntry> entries = _scanner.getFieldsAnnotatedWith(Foo.class);
        assertContainsField(entries, TestClass.class, "publicFoo");
        assertContainsField(entries, TestClass.class, "packageFoo");
        assertContainsField(entries, TestClass.class, "protectedFoo");
        assertContainsField(entries, TestClass.class, "privateFoo");
        assertNotContainsField(entries, TestClass.class, "publicBar");
        assertNotContainsField(entries, TestClass.class, "packageBar");
        assertNotContainsField(entries, TestClass.class, "protectedBar");
        assertNotContainsField(entries, TestClass.class, "privateBar");
    }

    @Test
    public void testAnnotatedStaticFields() {
        Collection<FieldEntry> entries = _scanner.getFieldsAnnotatedWith(Foo.class);
        assertContainsField(entries, TestClass.class, "publicStaticFoo");
        assertContainsField(entries, TestClass.class, "packageStaticFoo");
        assertContainsField(entries, TestClass.class, "protectedStaticFoo");
        assertContainsField(entries, TestClass.class, "privateStaticFoo");
        assertNotContainsField(entries, TestClass.class, "publicStaticBar");
        assertNotContainsField(entries, TestClass.class, "packageStaticBar");
        assertNotContainsField(entries, TestClass.class, "protectedStaticBar");
        assertNotContainsField(entries, TestClass.class, "privateStaticBar");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClassEntriesImmutable() {
        List<ClassEntry> entries = _scanner.getClassesAnnotatedWith(Foo.class);
        entries.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMethodEntriesImmutable() {
        List<MethodEntry> entries = _scanner.getMethodsAnnotatedWith(Foo.class);
        entries.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFieldEntriesImmutable() {
        List<FieldEntry> entries = _scanner.getFieldsAnnotatedWith(Foo.class);
        entries.clear();
    }

    private static void assertContainsType(Collection<ClassEntry> entries, Class<?> cls) {
        ClassEntry entry = new ClassEntry(cls.getName());
        assertTrue(entries.contains(entry));
    }

    private static void assertNotContainsType(Collection<ClassEntry> entries, Class<?> cls) {
        ClassEntry entry = new ClassEntry(cls.getName());
        assertFalse(entries.contains(entry));
    }

    private static void assertContainsMethod(Collection<MethodEntry> entries, Class<?> cls, String methodName) {
        MethodEntry entry = new MethodEntry(cls.getName(), methodName);
        assertTrue(entries.contains(entry));
    }

    private static void assertNotContainsMethod(Collection<MethodEntry> entries, Class<?> cls, String methodName) {
        MethodEntry entry = new MethodEntry(cls.getName(), methodName);
        assertFalse(entries.contains(entry));
    }

    private static void assertContainsField(Collection<FieldEntry> entries, Class<?> cls, String fieldName) {
        FieldEntry entry = new FieldEntry(cls.getName(), fieldName);
        assertTrue(entries.contains(entry));
    }

    private static void assertNotContainsField(Collection<FieldEntry> entries, Class<?> cls, String fieldName) {
        FieldEntry entry = new FieldEntry(cls.getName(), fieldName);
        assertFalse(entries.contains(entry));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
    public @interface Foo {}

    @SuppressWarnings("UnusedDeclaration")
    @Foo
    private static final class TestClass {
        /////////////////////////
        // Inner classes
        /////////////////////////
        @Foo public class PublicFoo {}
        @Foo class PackageFoo {}
        @Foo protected class ProtectedFoo {}
        @Foo private class PrivateFoo {}

        @Foo public static class PublicStaticFoo {}
        @Foo static class PackageStaticFoo {}
        @Foo protected static class ProtectedStaticFoo {}
        @Foo private static class PrivateStaticFoo {}

        public class PublicBar {}
        class PackageBar {}
        protected class ProtectedBar {}
        private class PrivateBar {}

        public static class PublicStaticBar {}
        static class PackageStaticBar {}
        protected static class ProtectedStaticBar {}
        private static class PrivateStaticBar {}

        /////////////////////////
        // Methods
        /////////////////////////
        @Foo public void publicFoo() {}
        @Foo void packageFoo() {}
        @Foo protected void protectedFoo() {}
        @Foo private void privateFoo() {}

        @Foo public static void publicStaticFoo() {}
        @Foo static void packageStaticFoo() {}
        @Foo protected static void protectedStaticFoo() {}
        @Foo private static void privateStaticFoo() {}

        public void publicBar() {}
        void packageBar() {}
        protected void protectedBar() {}
        private void privateBar() {}

        public static void publicStaticBar() {}
        static void packageStaticBar() {}
        protected static void protectedStaticBar() {}
        private static void privateStaticBar() {}

        /////////////////////////
        // Fields
        /////////////////////////
        @Foo public int publicFoo;
        @Foo int packageFoo;
        @Foo protected int protectedFoo;
        @Foo private int privateFoo;

        @Foo public static int publicStaticFoo;
        @Foo static int packageStaticFoo;
        @Foo protected static int protectedStaticFoo;
        @Foo private static int privateStaticFoo;

        public int publicBar;
        int packageBar;
        protected int protectedBar;
        private int privateBar;

        public static int publicStaticBar;
        static int packageStaticBar;
        protected static int protectedStaticBar;
        private static int privateStaticBar;
    }
}
