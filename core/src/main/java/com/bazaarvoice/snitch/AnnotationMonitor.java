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

import com.bazaarvoice.snitch.util.WorkQueue;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Defines a monitor that observes all loaded classes in the JVM and looks for static fields or methods that are marked
 * with a specific annotation.
 * <p/>
 * The design goal of this monitor is to be as unobtrusive to the class loading process as possible.  It shouldn't cause
 * unnecessary work to be performed each time a class is loaded into the system.  Along the same lines it shouldn't have
 * to index a class multiple times only to find the same annotated elements it saw the last time it was indexed.
 */
public class AnnotationMonitor {
    private static final MapMaker WEAK_KEY_MAP_MAKER = new MapMaker().weakKeys();

    /** Instrumentation API for interfacing with the JVM to determine which classes are loaded. */
    private final Instrumentation _instrumentation;

    /** The set of packages to check when looking for annotations. */
    private final Set<String> _packages;

    /** The set of class loaders that have loaded a class since we've last indexed. */
    private final WorkQueue<ClassLoader> _dirtyLoaders = new WorkQueue<ClassLoader>();

    /** The set of classes that have already been processed and should never be re-processed again. */
    private final Set<Class<?>> _processedClasses = Sets.newSetFromMap(WEAK_KEY_MAP_MAKER.<Class<?>, Boolean>makeMap());

    private final VariableClassProcessor _variableProcessor;
    private final FormatterClassProcessor _formatterProcessor;

    public AnnotationMonitor(Instrumentation instrumentation, Collection<String> packages,
                             Class<? extends Annotation> annotationClass,
                             NamingStrategy<? extends Annotation> namingStrategy) {
        _instrumentation = instrumentation;
        _packages = ImmutableSet.copyOf(packages);
        _variableProcessor = new VariableClassProcessor(annotationClass, namingStrategy);
        _formatterProcessor = new FormatterClassProcessor();

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
     * the provided class loader.  The variables are provided in sorted order.
     */
    public List<Variable> getVariables(ClassLoader loader) {
        // Catch up with any queued work to make sure we give the most accurate picture possible
        processWorkQueue();

        List<Variable> variables = _variableProcessor.getVariables(loader);
        Collections.sort(variables, new Comparator<Variable>() {
            @Override
            public int compare(Variable a, Variable b) {
                return ComparisonChain.start()
                        .compare(a.getName(), b.getName())
                        .result();
            }
        });
        return variables;
    }

    /** Retrieve the formatter for the provided class. */
    public <T> Formatter<T> getFormatter(Class<T> cls) {
        // Catch up with any queued work to make sure we give the most accurate picture possible
        processWorkQueue();

        return _formatterProcessor.getFormatter(cls);
    }

    private void onClassLoaded(ClassLoader loader, String dirName) {
        if (isClassDirNameInMonitoredPackage(dirName)) {
            _dirtyLoaders.add(loader);
        }
    }

    private void processWorkQueue() {
        List<ClassProcessor> processors = ImmutableList.of(_variableProcessor, _formatterProcessor);

        for (ClassLoader loader : _dirtyLoaders.removeAll()) {
            for (Class cls : _instrumentation.getInitiatedClasses(loader)) {
                if (!_processedClasses.add(cls) || !isClassInMonitoredPackage(cls)) {
                    continue;
                }

                for (ClassProcessor processor : processors) {
                    processor.process(loader, cls);
                }
            }
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
