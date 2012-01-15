/*
 * Copyright (c) 2012 Bazaarvoice
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
import com.bazaarvoice.snitch.NamingStrategy;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class DefaultNamingStrategyTest {
    private NamingStrategy _strategy;

    @Before
    public void setup() {
        _strategy = new DefaultNamingStrategy();
    }

    @Test
    public void testFieldWithNoAnnotationName() throws NoSuchFieldException {
        Field f = TestClass.class.getDeclaredField("noName");

        assertEquals("noName", _strategy.getName(f));
    }

    @Test
    public void testFieldWithAnnotationName() throws NoSuchFieldException {
        Field f = TestClass.class.getDeclaredField("withName");

        assertEquals("custom-name", _strategy.getName(f));
    }

    @Test
    public void testMethodWithNoAnnotationName() throws NoSuchMethodException {
        Method m = TestClass.class.getDeclaredMethod("noNameMethod");

        assertEquals("noNameMethod", _strategy.getName(m));
    }

    @Test
    public void testGetMethodWithNoAnnotationName() throws NoSuchMethodException {
        Method m = TestClass.class.getDeclaredMethod("getNoNameGetMethod");

        assertEquals("noNameGetMethod", _strategy.getName(m));
    }

    @Test
    public void testIsMethodWithNoAnnotationName() throws NoSuchMethodException {
        Method m = TestClass.class.getDeclaredMethod("isNoNameIsMethod");

        assertEquals("noNameIsMethod", _strategy.getName(m));
    }

    @Test
    public void testMethodWithAnnotationName() throws NoSuchMethodException {
        Method m = TestClass.class.getDeclaredMethod("getWithNameMethod");

        assertEquals("custom-name-method", _strategy.getName(m));
    }

    @SuppressWarnings ({"UnusedDeclaration"})
    private static final class TestClass {
        @Monitored
        private static final int noName = 0;

        @Monitored ("custom-name")
        private static final int withName = 2;

        @Monitored
        private static int noNameMethod() {
            return 1;
        }

        @Monitored
        private static int getNoNameGetMethod() {
            return 3;
        }

        @Monitored
        private static int isNoNameIsMethod() {
            return 5;
        }

        @Monitored ("custom-name-method")
        private static int getWithNameMethod() {
            return 11;
        }
    }
}
