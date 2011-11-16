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

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration"}) // This class is an entry point
public class MonitoringAgent {
    /** Default annotation to look for if one isn't specified in the agent arguments. */
    private static final Class<? extends Annotation> DEFAULT_ANNOTATION = Monitored.class;

    /** Default naming strategy to use if one isn't specified in the agent arguments. */
    private static final NamingStrategy<Annotation> DEFAULT_NAMING_STRATEGY = new DefaultNamingStrategy<Annotation>();

    /** Agent arguments are separated by commas with keys and values separated by equals. */
    private static final Splitter.MapSplitter ARG_SPLITTER = Splitter.on(",")
            .omitEmptyStrings()
            .trimResults()
            .withKeyValueSeparator("=");

    /** Packages are UNIX/Classpath style and separated by colons. */
    private static final Splitter PACKAGE_SPLITTER = Splitter.on(":")
            .omitEmptyStrings()
            .trimResults();

    /** The global instance of the monitor. */
    private static AnnotationMonitor INSTANCE;

    /** Main entry point for the agent. */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        Collection<String> packages = Lists.newArrayList();
        Class<? extends Annotation> annotationClass = DEFAULT_ANNOTATION;
        NamingStrategy<? extends Annotation> namingStrategy = DEFAULT_NAMING_STRATEGY;

        // Parse arguments...
        Map<String, String> args = ARG_SPLITTER.split(agentArgs);
        for (Map.Entry<String, String> entry : args.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            if ("packages".equals(name)) {
                for (String pkg : PACKAGE_SPLITTER.split(value)) {
                    if (!pkg.endsWith(".")) {
                        // Make sure all packages end with a dot to prevent partial prefix matches
                        pkg += '.';
                    }
                    packages.add(pkg);
                }
            } else if ("annotation".equals(name)) {
                annotationClass = loadSubclass(value, Annotation.class);
            } else if ("naming".equals(name)) {
                //noinspection unchecked
                namingStrategy = newSubclassInstance(value, NamingStrategy.class);
            }
        }

        INSTANCE = new AnnotationMonitor(instrumentation, packages, annotationClass, namingStrategy);
    }

    /** Retrieve the global, JVM-wide instance of the monitor. */
    public AnnotationMonitor getMonitor() {
        return INSTANCE;
    }

    private static <T> Class<? extends T> loadSubclass(String name, Class<T> superClass) {
        try {
            return Class.forName(name).asSubclass(superClass);
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }

    private static <T> T newSubclassInstance(String name, Class<T> superClass) {
        Class<? extends T> cls = loadSubclass(name, superClass);
        try {
            return cls.newInstance();
        } catch (InstantiationException e) {
            throw Throwables.propagate(e);
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }
}
