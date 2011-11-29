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

import com.bazaarvoice.snitch.util.VariableIndex;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class VariableClassProcessor implements ClassProcessor {
    /** The annotation class to find. */
    private final Class<? extends Annotation> _annotationClass;

    /** The naming strategy to use to give names to newly discovered variables. */
    private final NamingStrategy<? extends Annotation> _namingStrategy;

    /** The index of variables keyed by the class loader that loaded them. */
    private final Cache<ClassLoader, VariableIndex> _indices = CacheBuilder
            .newBuilder()
            .weakKeys()
            .build(new CacheLoader<ClassLoader, VariableIndex>() {
                @Override
                public VariableIndex load(ClassLoader key) throws Exception {
                    return new VariableIndex(_annotationClass, _namingStrategy);
                }
            });

    /** A lock that is used while working with the index. */
    private final ReadWriteLock _lock = new ReentrantReadWriteLock();

    public VariableClassProcessor(Class<? extends Annotation> annotationClass,
                                  NamingStrategy<? extends Annotation> namingStrategy) {
        _annotationClass = annotationClass;
        _namingStrategy = namingStrategy;
    }

    @Override
    public void process(ClassLoader loader, Class<?> cls) {
        _lock.writeLock().lock();
        try {
            VariableIndex index = _indices.getUnchecked(loader);
            index.addClass(cls);
        } finally {
            _lock.writeLock().unlock();
        }

    }

    public List<Variable> getVariables(ClassLoader loader) {
        List<Variable> variables = Lists.newArrayList();

        _lock.readLock().lock();
        try {
            while (loader != null) {
                variables.addAll(_indices.getUnchecked(loader).getVariables());
            }
        } finally {
            _lock.readLock().unlock();
        }

        return variables;
    }
}
