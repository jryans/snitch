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

import com.google.common.base.Throwables;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * A utility class that can be used to determine whether or not a class has been loaded or not.  This implementation
 * will not cause classes to be loaded if they aren't already loaded.
 * <p/>
 * This implementation isn't the most elegant because it relies on reflection and the ability to call
 * <code>setAccessible(true)</code> on the final protected {@link ClassLoader#findLoadedClass} method.  Unfortunately
 * there really isn't a better way to do this without forcing the entire application to load all classes with a custom
 * class loader or requiring the use of a java agent.
 */
public class ClassLoaderHelper {
    private final ClassLoader _loader;
    private final Method _findLoadedClassMethod;

    public ClassLoaderHelper() {
        this(ClassLoader.getSystemClassLoader());
    }

    public ClassLoaderHelper(ClassLoader loader) {
        _loader = loader;

        try {
            _findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            _findLoadedClassMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }
    }

    /** Determine whether or not a class has been loaded. */
    public boolean isClassLoaded(String className) {
        return findLoadedClass(className) != null;
    }

    /**
     * Finds the given class if it's already loaded in the class loader.
     *
     * @return The class if loaded, otherwise <tt>null</tt>
     */
    @Nullable
    public <T extends Class<?>> T findLoadedClass(String className) {
        try {
            //noinspection unchecked
            return (T) _findLoadedClassMethod.invoke(_loader, className);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
