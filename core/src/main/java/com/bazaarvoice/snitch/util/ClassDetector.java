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
package com.bazaarvoice.snitch.util;

public interface ClassDetector {
    /** Determine whether or not a class has been loaded. */
    boolean isClassLoaded(String className);

    /**
     *  Return the class object for a class that has been loaded.
     *  <p/>
     *  If the class hasn't yet been loaded calling this method may trigger it to be loaded.  If the class cannot be
     *  found, loaded, or initialized then {@code null} will be returned.
     */
    Class<?> getLoadedClass(String className);
}
