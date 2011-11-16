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
import com.bazaarvoice.snitch.util.WorkQueue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Defines a monitor that observes all loaded classes in the JVM and looks for static fields or methods that are marked
 * with a specific annotation.
 * <p/>
 * The design goal of this monitor is to be as unobtrusive to the class loading process as possible.  It shouldn't cause
 * unnecessary work to be performed each time a class is loaded into the system.  Along the same lines it shouldn't have
 * to index a class multiple times only to find the same annotated elements it saw the last time it was indexed.
 */
public class AnnotationMonitor {
    /** Instrumentation API for interfacing with the JVM to determine which classes are loaded. */
    private final Instrumentation _instrumentation;

    /** The set of packages to check when looking for annotations. */
    private final Set<String> _packages;

    /** The annotation class to find. */
    private final Class<? extends Annotation> _annotationClass;

    /** The naming strategy to use to give names to newly discovered variables. */
    private final NamingStrategy<? extends Annotation> _namingStrategy;

    /** The set of class loaders that have loaded a class since we've last indexed. */
    private final WorkQueue<ClassLoader> _dirtyLoaders = new WorkQueue<ClassLoader>();

    /** The index of variables keyed by the class loader that loaded them. */
    private final ConcurrentMap<ClassLoader, VariableIndex> _indices = new MapMaker().weakKeys().makeMap();

    /** A lock that is used while working with the index. */
    private final ReadWriteLock _indexingLock = new ReentrantReadWriteLock();

    public AnnotationMonitor(Instrumentation instrumentation, Collection<String> packages,
                             Class<? extends Annotation> annotationClass,
                             NamingStrategy<? extends Annotation> namingStrategy) {
        _instrumentation = instrumentation;
        _packages = ImmutableSet.copyOf(packages);
        _annotationClass = annotationClass;
        _namingStrategy = namingStrategy;

        // Register a transformer with the instrumentation framework so that we see when a class loader loads something
        instrumentation.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String dirName, Class<?> cls, ProtectionDomain domain,
                                    byte[] bytes) throws IllegalClassFormatException {
                onClassLoaded(loader, dirName);
                return null;
            }
        });
    }

    /**
     * Retrieve all of the annotated variables that the monitor has discovered that occur in classes that are visible to
     * the provided class loader.
     */
    public List<Variable> getVariables(ClassLoader loader) {
        // Catch up with any queued work to make sure we give the most accurate picture possible
        processWorkQueue();

        _indexingLock.readLock().lock();
        try {
            List<Variable> variables = Lists.newArrayList();

            // Walk up the chain of loaders since this loader's ancestors could have loaded classes on behalf of it
            while (loader != null) {
                VariableIndex index = _indices.get(loader);
                if (index != null) {
                    variables.addAll(index.getVariables());
                }
                loader = loader.getParent();
            }

            return variables;
        } finally {
            _indexingLock.readLock().unlock();
        }
    }

    private void processWorkQueue() {
        Set<ClassLoader> loaders = _dirtyLoaders.removeAll();
        if (loaders.isEmpty()) {
            return;
        }

        _indexingLock.writeLock().lock();
        try {
            for (ClassLoader loader : loaders) {
                VariableIndex index = _indices.get(loader);
                if (index == null) {
                    index = new VariableIndex(_annotationClass, _namingStrategy);
                    _indices.put(loader, index);
                }

                for (Class cls : _instrumentation.getInitiatedClasses(loader)) {
                    if (cls.getClassLoader() == loader && isClassInMonitoredPackage(cls)) {
                        index.addClass(cls);
                    }
                }
            }
        } finally {
            _indexingLock.writeLock().unlock();
        }
    }

    private void onClassLoaded(ClassLoader loader, String dirName) {
        if (isClassDirNameInMonitoredPackage(dirName)) {
            _dirtyLoaders.add(loader);
        }
    }

    private boolean isClassNameInMonitoredPackage(String className) {
        // If we weren't given any packages then we have to scan everything
        if (_packages.isEmpty()) {
            return true;
        }

        for (String pkg : _packages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClassInMonitoredPackage(Class<?> cls) {
        return isClassNameInMonitoredPackage(cls.getName());
    }

    private boolean isClassDirNameInMonitoredPackage(String dirName) {
        String name = dirName.replace('/', '.');
        return isClassNameInMonitoredPackage(name);
    }
}
