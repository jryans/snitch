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

package com.bazaarvoice.snitch.processors;

import com.bazaarvoice.snitch.NamingStrategy;
import com.bazaarvoice.snitch.Variable;
import com.bazaarvoice.snitch.variables.FieldVariable;
import com.bazaarvoice.snitch.variables.MethodVariable;
import com.google.common.base.Throwables;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class VariableAnnotationProcessor {
    /** The naming strategy to use to give names to newly discovered variables. */
    private final NamingStrategy _namingStrategy;

    public VariableAnnotationProcessor(NamingStrategy namingStrategy) {
        _namingStrategy = namingStrategy;
    }

    public Variable<?> createFieldVariable(Class<?> cls, String fieldName) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            return new FieldVariable(cls, field, _namingStrategy.getName(field));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Variable<?> createMethodVariable(Class<?> cls, String methodName) {
        try {
            Method method = cls.getDeclaredMethod(methodName);
            return new MethodVariable(cls, method, _namingStrategy.getName(method));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
