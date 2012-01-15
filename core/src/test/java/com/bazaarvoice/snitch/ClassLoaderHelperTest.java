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
package com.bazaarvoice.snitch;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClassLoaderHelperTest {
    private static final String CLASS_NAME_THAT_DOES_NOT_EXIST = "ClassThatDoesNotExist";
    private static final String CLASS_NAME_THAT_IS_NOT_LOADED = "java.awt.ActiveEvent";
    private static final String CLASS_NAME_THAT_IS_LOADED = String.class.getName();

    private final ClassLoaderHelper _helper = new ClassLoaderHelper();

    @Test
    public void testClassThatDoesNotExist() {
        assertFalse(_helper.isClassLoaded(CLASS_NAME_THAT_DOES_NOT_EXIST));
        assertNull(_helper.findLoadedClass(CLASS_NAME_THAT_DOES_NOT_EXIST));
    }

    @Test
    public void testClassThatIsNotLoaded() {
        assertFalse(_helper.isClassLoaded(CLASS_NAME_THAT_IS_NOT_LOADED));
        assertNull(_helper.findLoadedClass(CLASS_NAME_THAT_IS_NOT_LOADED));
    }

    @Test
    public void testClassThatIsLoaded() {
        assertTrue(_helper.isClassLoaded(CLASS_NAME_THAT_IS_LOADED));
        assertNotNull(_helper.findLoadedClass(CLASS_NAME_THAT_IS_LOADED));
    }
}
