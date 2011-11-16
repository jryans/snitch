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

import com.bazaarvoice.snitch.FieldVariable;
import com.bazaarvoice.snitch.MethodVariable;
import com.bazaarvoice.snitch.NamingStrategy;
import com.bazaarvoice.snitch.Variable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class VariableIndex {
    /** The annotation to find in classes that are added. */
    private final Class<? extends Annotation> _annotationClass;

    /** The strategy used for naming variables that are discovered. */
    private final NamingStrategy _namingStrategy;

    /** The set of classes that have already been processed and should never be re-processed again. */
    private final Set<Class<?>> _seenClasses = Sets.newSetFromMap(Maps.<Class<?>, Boolean>newHashMap());

    /** The set of variables that have been discovered. */
    private final List<Variable> _variables = Lists.newArrayList();

    public VariableIndex(Class<? extends Annotation> annotationClass,
                         NamingStrategy<? extends Annotation> namingStrategy) {
        _annotationClass = annotationClass;
        _namingStrategy = namingStrategy;
    }

    /**
     * Add the variables found in the provided class to the index.
     * <p/>
     * NOTE: This method is not thread safe.
     */
    @SuppressWarnings({"unchecked"})
    public void addClass(Class<?> cls) {
        if (!_seenClasses.add(cls)) {
            return;
        }

        for (Field field : cls.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || Modifier.isNative(modifiers) || field.isSynthetic()) {
                continue;
            }

            Annotation annotation = field.getAnnotation(_annotationClass);
            if (annotation == null) {
                continue;
            }

            // This makes an unchecked call into the naming strategy as it's a generic class.  We store an unbound
            // instance of it because we don't know at compile time which subclass of Annotation we'll be working with.
            // The user of this class guarantees for us that the annotation class and naming strategy provided are
            // compatible.  If that guarantee isn't true then this could throw a runtime exception.
            String name = _namingStrategy.getName(field, annotation);
            Variable variable = new FieldVariable(cls, field, name);

            _variables.add(variable);
        }

        for (Method method : cls.getDeclaredMethods()) {
            if (method.getParameterTypes().length != 0) {
                continue;
            }

            if (method.getReturnType() == void.class) {
                continue;
            }
            
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) || Modifier.isNative(modifiers) || method.isSynthetic()) {
                continue;
            }

            Annotation annotation = method.getAnnotation(_annotationClass);
            if (annotation == null) {
                continue;
            }

            // Again, this makes an unchecked call into the naming strategy and may throw a runtime exception.
            String name = _namingStrategy.getName(method, annotation);
            Variable variable = new MethodVariable(cls, method, name);

            _variables.add(variable);
        }
    }

    /**
     * Retrieve all of the variables that have been found.
     * <p/>
     * NOTE: This method is thread safe to multiple readers, but not to simultaneous readers and writers.
     */
    public Collection<Variable> getVariables() {
        return ImmutableList.copyOf(_variables);
    }
}
