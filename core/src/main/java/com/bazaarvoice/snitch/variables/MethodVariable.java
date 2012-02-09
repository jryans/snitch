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
package com.bazaarvoice.snitch.variables;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;

import java.lang.ref.Reference;
import java.lang.reflect.Method;

public class MethodVariable extends AbstractVariable {
    private final Method _method;

    public MethodVariable(Class<?> owner, String name, Reference<Object> instance, Method method) {
        super(owner, name, instance);

        _method = method;
        _method.setAccessible(true);
    }

    public MethodVariable(Class<?> owner, String name, Method method) {
        this(owner, name, null, method);
    }
    
    @VisibleForTesting
    public Method getMethod() {
        return _method;
    }

    @Override
    public Class<?> getType() {
        return _method.getReturnType();
    }

    @Override
    public Object getValue() {
        try {
            return _method.invoke(_instance);
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
                .add("name", _name)
                .add("owner", _owner)
                .add("instance", _instance.get())
                .add("method", _method)
                .add("value", getValue())
                .toString();
    }
}
