/*
 * Copyright 2011 Bazaarvoice
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
package com.bazaarvoice.snitch.util;

import com.bazaarvoice.snitch.DefaultNamingStrategy;
import com.bazaarvoice.snitch.Variable;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.Assert.*;

public class VariableIndexTest {
    private VariableIndex _fieldIndex;
    private VariableIndex _methodIndex;

    @Before
    public void setup() {
        _fieldIndex = new VariableIndex(Marked.class, new DefaultNamingStrategy<Marked>());
        _fieldIndex.addClass(Fields.class);

        _methodIndex = new VariableIndex(Marked.class, new DefaultNamingStrategy<Marked>());
        _methodIndex.addClass(Methods.class);
    }

    @Test
    public void testMarkedFields() {
        // Static so these should show up
        assertNotNull("m1", findFieldVariable("m1"));
        assertNotNull("m2", findFieldVariable("m2"));
        assertNotNull("m3", findFieldVariable("m3"));
        assertNotNull("m4", findFieldVariable("m4"));

        // Non-static so shouldn't appear
        assertNull("m5", findFieldVariable("m5"));
        assertNull("m6", findFieldVariable("m6"));
        assertNull("m7", findFieldVariable("m7"));
        assertNull("m8", findFieldVariable("m8"));
    }

    @Test
    public void testUnmarkedFields() {
        // All of these are unmarked, so none of them should show up
        assertNull("u1", findFieldVariable("u1"));
        assertNull("u2", findFieldVariable("u2"));
        assertNull("u3", findFieldVariable("u3"));
        assertNull("u4", findFieldVariable("u4"));
        assertNull("u5", findFieldVariable("u5"));
        assertNull("u6", findFieldVariable("u6"));
        assertNull("u7", findFieldVariable("u7"));
        assertNull("u8", findFieldVariable("u8"));
    }

    @Test
    public void testMarkedFieldsValue() {
        assertEquals("m1", 1, findFieldVariable("m1").getValue());
        assertEquals("m2", 2, findFieldVariable("m2").getValue());
        assertEquals("m3", 3, findFieldVariable("m3").getValue());
        assertEquals("m4", 4, findFieldVariable("m4").getValue());
    }

    @Test
    public void testMarkedMethods() {
        // Static, with no arguments so these should show up
        assertNotNull("m1", findMethodVariable("m1"));
        assertNotNull("m2", findMethodVariable("m2"));
        assertNotNull("m3", findMethodVariable("m3"));
        assertNotNull("m4", findMethodVariable("m4"));

        // Non-static so shouldn't appear
        assertNull("m5", findMethodVariable("m5"));
        assertNull("m6", findMethodVariable("m6"));
        assertNull("m7", findMethodVariable("m7"));
        assertNull("m8", findMethodVariable("m8"));

        // Static, but they have arguments so they shouldn't appear
        assertNull("m9", findMethodVariable("m9"));
        assertNull("m10", findMethodVariable("m10"));
        assertNull("m11", findMethodVariable("m11"));
        assertNull("m12", findMethodVariable("m12"));

        // Static, but they return void so they shouldn't appear
        assertNull("m13", findMethodVariable("m13"));
        assertNull("m14", findMethodVariable("m14"));
        assertNull("m15", findMethodVariable("m15"));
        assertNull("m16", findMethodVariable("m16"));
    }

    @Test
    public void testUnmarkedMethods() {
        // All of these are unmarked, so none of them should show up
        assertNull("u1", findMethodVariable("u1"));
        assertNull("u2", findMethodVariable("u2"));
        assertNull("u3", findMethodVariable("u3"));
        assertNull("u4", findMethodVariable("u4"));
        assertNull("u5", findMethodVariable("u5"));
        assertNull("u6", findMethodVariable("u6"));
        assertNull("u7", findMethodVariable("u7"));
        assertNull("u8", findMethodVariable("u8"));
    }

    @Test
    public void testMarkedMethodsValue() {
        assertEquals("m1", 1, findMethodVariable("m1").getValue());
        assertEquals("m2", 2, findMethodVariable("m2").getValue());
        assertEquals("m3", 3, findMethodVariable("m3").getValue());
        assertEquals("m4", 4, findMethodVariable("m4").getValue());
    }

    private Variable findFieldVariable(String name) {
        return findVariable(_fieldIndex, name);
    }

    private Variable findMethodVariable(String name) {
        return findVariable(_methodIndex, name);
    }

    private Variable findVariable(VariableIndex index, String name) {
        for (Variable variable : index.getVariables()) {
            if (name.equals(variable.getName())) {
                return variable;
            }
        }
        return null;
    }

    @Retention (RetentionPolicy.RUNTIME)
    private @interface Marked {}

    @SuppressWarnings ({"UnusedDeclaration"})
    private static final class Fields {
        //////////////////
        // Marked fields
        //////////////////

        // Static
        @Marked public static int m1 = 1;
        @Marked protected static int m2 = 2;
        @Marked static int m3 = 3;
        @Marked private static int m4 = 4;
        
        // Non-static
        @Marked public int m5 = 5;
        @Marked protected int m6 = 6;
        @Marked int m7 = 7;
        @Marked private int m8 = 8;


        //////////////////
        // Unmarked fields
        //////////////////

        // Static
        public static int u1 = 1;
        protected static int u2 = 2;
        static int u3 = 3;
        private static int u4 = 4;

        // Non-static
        public int u5 = 5;
        protected int u6 = 2;
        int u7 = 3;
        private int u8 = 4;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private static final class Methods {
        ///////////////////
        // Marked methods
        ///////////////////

        // Static
        @Marked public static int m1() { return 1; }
        @Marked protected static int m2() { return 2; }
        @Marked static int m3() { return 3; }
        @Marked private static int m4() { return 4; }

        // Non-static
        @Marked public int m5() { return 5; }
        @Marked protected int m6() { return 6; }
        @Marked int m7() { return 7; }
        @Marked private int m8() { return 8; }

        // With arguments
        @Marked public static int m9(int i) { return 9; }
        @Marked protected static int m10(int i) { return 10; }
        @Marked static int m11(int i) { return 11; }
        @Marked private static int m12(int i) { return 12; }

        // Void returning methods
        @Marked public static void m13() {}
        @Marked protected static void m14() {}
        @Marked static void m15() {}
        @Marked private static void m16() {}

        ///////////////////
        // Unmarked methods
        ///////////////////

        // Static
        public static int u1() { return 1; }
        protected static int u2() { return 2; }
        static int u3() { return 3; }
        private static int u4() { return 4; }

        // Non-static
        public int u5() { return 5; }
        protected int u6() { return 6; }
        int u7() { return 7; }
        private int u8() { return 8; }
    }
}
