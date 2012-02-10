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

import com.bazaarvoice.snitch.Variable;

import java.lang.ref.WeakReference;

abstract class AbstractVariable implements Variable {
    protected final Class<?> _owner;
    protected final String _name;
    protected final WeakReference<Object> _instance;

    protected AbstractVariable(Class<?> owner, String name, WeakReference<Object> instance) {
        _owner = owner;
        _name = name;
        _instance = instance;
    }

    @Override
    public final Class<?> getOwner() {
        return _owner;
    }

    @Override
    public final String getName() {
        return _name;
    }

    @Override
    public Object getInstance() {
        return (_instance != null) ? _instance.get() : null;
    }
}
