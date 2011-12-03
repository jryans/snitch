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
import com.google.common.collect.Maps;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentMap;

public class FormatterClassProcessor implements ClassProcessor {
    private final ConcurrentMap<Class<?>, Formatter<?>> _formatters = Maps.newConcurrentMap();

    @Override
    public void process(ClassLoader loader, Class<?> cls) {
        // Check for the formatter annotation...
        FormattedBy annotation = cls.getAnnotation(FormattedBy.class);
        if (annotation != null) {
            Class<? extends Formatter> formatClass = annotation.value();
            Formatter formatter = newInstance(formatClass);
            _formatters.put(cls, formatter);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <T> Formatter<T> getFormatter(Class<T> cls) {
        Formatter formatter = _formatters.putIfAbsent(cls, ToStringFormatter.INSTANCE);
        if (formatter == null) {
            formatter = ToStringFormatter.INSTANCE;
        }

        return formatter;
    }

    private <T> T newInstance(Class<T> cls) {
        // Find the default constructor for the class
        Constructor<T> constructor;
        try {
            constructor = cls.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw Throwables.propagate(e);
        }

        // Make sure we can call it
        constructor.setAccessible(true);

        try {
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw Throwables.propagate(e);
        } catch (InstantiationException e) {
            throw Throwables.propagate(e);
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}