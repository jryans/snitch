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
package com.bazaarvoice.snitch.formatters;

import com.bazaarvoice.snitch.FormattedBy;
import com.bazaarvoice.snitch.Formatter;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

// TODO: Possibly an annotation parameter that indicates that a formatter has state and a new one per instance is needed
// TODO: Use a library for creating instances, objenesis?
// TODO: An error reporter, likely the same one that VariableRegistry uses
public class FormatterRegistry {
    /** The default formatter instance to use whenever a formatter can't be found for a monitored class. */
    private Formatter<?> _defaultFormatter;

    /** Mapping from class to the formatter for that class.  We use weak keys to ensure that classes can be unloaded. */
    private final LoadingCache<Class<?>, Formatter<?>> _formatters = CacheBuilder.newBuilder()
        .weakKeys()
        .build(new CacheLoader<Class<?>, Formatter<?>>() {
            @Override
            public Formatter<?> load(Class<?> cls) throws Exception {
                // Check to see if this class is annotated with a FormattedBy annotation
                FormattedBy annotation = cls.getAnnotation(FormattedBy.class);
                if (annotation != null) {
                    Class<? extends Formatter> formatterClass = annotation.value();
                    return newInstance(formatterClass);
                }

                // Otherwise just use the default formatter instance
                return _defaultFormatter;
            }

            private <T> T newInstance(Class<T> cls) throws Exception {
                // Find the default constructor for the class
                Constructor<T> constructor;
                try {
                    constructor = cls.getDeclaredConstructor();
                } catch (NoSuchMethodException e) {
                    throw Throwables.propagate(e);
                }

                // Make sure we can call it
                constructor.setAccessible(true);

                return constructor.newInstance();
            }
        });

    /**
     * Formatter overrides.  These are stored separately from the cached formatters because the cached formatters could
     * be unloaded at any time and recomputed later.  We need to make sure that an override never gets unloaded.  We use
     * weak keys to ensure that classes can be unloaded.
     */
    private final ConcurrentMap<Class<?>, Formatter<?>> _formatterOverrides = new MapMaker()
        .weakKeys()
        .makeMap();

    public FormatterRegistry(Formatter<?> defaultFormatter) {
        _defaultFormatter = defaultFormatter;
    }

    /** Return a formatter instance that should be used when formatting the specified class. */
    @SuppressWarnings("unchecked")
    public <T> Formatter<T> getFormatter(Class<T> cls) {
        Preconditions.checkNotNull(cls);

        Formatter<T> override = (Formatter<T>) _formatterOverrides.get(cls);
        if (override != null) {
            return override;
        }

        try {
            return (Formatter<T>) _formatters.get(cls);
        } catch (ExecutionException e) {
            return (Formatter<T>) _defaultFormatter;
        }
    }

    /** Register a specific formatter to be used for a class. */
    public <T> void registerFormatter(Class<T> cls, Formatter<T> formatter) {
        _formatterOverrides.put(cls, formatter);
    }

    /** Set the default formatter to use if no other formatter can be located. */
    public void setDefaultFormatter(Formatter<?> formatter) {
        _defaultFormatter = formatter;
    }
}
