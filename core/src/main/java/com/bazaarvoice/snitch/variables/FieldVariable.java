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

package com.bazaarvoice.snitch.variables;

import com.bazaarvoice.snitch.Variable;
import com.google.common.base.Objects;

import java.lang.reflect.Field;

public class FieldVariable implements Variable {
    private final Class<?> _owner;
    private final Field _field;
    private final String _name;

    public FieldVariable(Class<?> owner, Field field, String name) {
        _owner = owner;
        _field = field;
        _name = name;

        _field.setAccessible(true);
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
        return _field.getType();
    }

    @Override
    public Object getValue() {
        try {
            // It's okay to pass null into the get call because this field is static
            return _field.get(null);
        } catch (Exception e) {
            // If we weren't able to access the field then we need to notify the caller.  Probably the easiest way is
            // to return the exception itself as the value of the variable.  This will show the user that it wasn't
            // able to be accessed without any possibility of crashing the program.
            return e;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("owner", _owner)
                .add("field", _field)
                .add("name", _name)
                .add("value", getValue())
                .toString();
    }
}
