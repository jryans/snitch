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

import com.google.common.base.Objects;

import java.lang.reflect.Method;

public class MethodVariable implements Variable {
    private final Class<?> _owner;
    private final Method _method;
    private final String _name;

    public MethodVariable(Class<?> owner, Method method, String name) {
        _owner = owner;
        _method = method;
        _name = name;

        _method.setAccessible(true);
    }

    @Override
    public Class<?> getOwner() {
        return _owner;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Class<?> getType() {
        return _method.getReturnType();
    }

    @Override
    public Object getValue() {
        try {
            // It's okay to pass null into the invoke call because this method is static
            return _method.invoke(null);
        } catch (Exception e) {
            // If we weren't able to invoke the method then we need to notify the caller.  Probably the easiest way is
            // to return the exception itself as the value of the variable.  This will show the user that it wasn't
            // able to be accessed without any possibility of crashing the program.
            return e;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("owner", _owner)
                .add("method", _method)
                .add("name", _name)
                .add("value", getValue())
                .toString();
    }
}
