/*
 * Copyright 2012 Bazaarvoice
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

import com.bazaarvoice.snitch.config.Configuration;
import com.bazaarvoice.snitch.config.DefaultConfiguration;
import com.bazaarvoice.snitch.formatters.DefaultFormatter;
import com.bazaarvoice.snitch.formatters.FormatterRegistry;
import com.bazaarvoice.snitch.naming.DefaultNamingStrategy;
import com.bazaarvoice.snitch.naming.NamingStrategy;
import com.bazaarvoice.snitch.scanner.AnnotationScanner;
import com.bazaarvoice.snitch.scanner.ClassPathAnnotationScanner;
import com.bazaarvoice.snitch.variables.VariableRegistry;
import com.google.common.base.Throwables;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class Snitch {
    private static final Class<? extends Annotation> DEFAULT_ANNOTATION_CLASS = Monitored.class;
    private static final Formatter DEFAULT_FORMATTER = DefaultFormatter.INSTANCE;
    private static final NamingStrategy DEFAULT_NAMING_STRATEGY = DefaultNamingStrategy.INSTANCE;

    private static Snitch _instance;

    public synchronized static Snitch getInstance() {
        if (_instance == null) {
            try {
                _instance = new Snitch(new DefaultConfiguration());
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        return _instance;
    }

    /** Initialize snitch with the specified configuration. */
    public synchronized static Snitch initialize(Configuration config) {
        try {
            _instance = new Snitch(config);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return _instance;
    }

    private final VariableRegistry _variableRegistry;
    private final FormatterRegistry _formatterRegistry;

    @SuppressWarnings("unchecked")
    private Snitch(Configuration config) throws Exception {
        AnnotationScanner annotationScanner;
        List<String> packagesToScan = config.getPackagesToScan();
        if (packagesToScan != null && !packagesToScan.isEmpty()) {
            String[] packages = packagesToScan.toArray(new String[packagesToScan.size()]);
            annotationScanner = new ClassPathAnnotationScanner(packages);
        } else {
            annotationScanner = new ClassPathAnnotationScanner();
        }

        Class<? extends Annotation> annotationClass = loadAnnotationClass(config);
        NamingStrategy<? extends Annotation> namingStrategy = loadNamingStrategy(config);
        _variableRegistry = new VariableRegistry(annotationClass, annotationScanner, namingStrategy);

        Formatter defaultFormatter = loadDefaultFormatter(config);
        _formatterRegistry = new FormatterRegistry(defaultFormatter);
        for (Map.Entry<String, String> entry : config.getFormatterClassNames().entrySet()) {
            Class cls = loadClass(entry.getKey(), Object.class);
            Formatter formatter = loadFormatter(entry.getValue());

            _formatterRegistry.registerFormatter(cls, formatter);
        }
    }
    
    public List<Variable> getVariables() {
        return _variableRegistry.getVariables();
    }
    
    public Formatter<?> getFormatter(Variable variable) {
        Class<?> cls = variable.getType();
        return _formatterRegistry.getFormatter(cls);
    }
    
    public void registerInstance(Object instance) {
        _variableRegistry.registerInstance(instance);
    }

    public <T> void registerFormatter(Class<T> cls, Formatter<T> formatter) {
        _formatterRegistry.registerFormatter(cls, formatter);
    }

    private static Class<? extends Annotation> loadAnnotationClass(Configuration config) throws ClassNotFoundException {
        String className = config.getAnnotationClassName();
        if (className != null) {
            return loadClass(className, Annotation.class);
        }

        return DEFAULT_ANNOTATION_CLASS;
    }

    private static NamingStrategy loadNamingStrategy(Configuration config)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String className = config.getNamingStrategyClassName();
        if (className == null) {
            return DEFAULT_NAMING_STRATEGY;
        }
        
        Class<? extends NamingStrategy> cls = loadClass(className, NamingStrategy.class);
        return newInstance(cls);
    }
    
    private static Formatter loadDefaultFormatter(Configuration config)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String className = config.getDefaultFormatterClassName();
        if (className == null) {
            return DEFAULT_FORMATTER;
        }

        Class<? extends Formatter> cls = loadClass(className, Formatter.class);
        return newInstance(cls);
    }

    private static Formatter loadFormatter(String className)
            throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends Formatter> cls = loadClass(className, Formatter.class);
        return newInstance(cls);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> loadClass(String className, Class<T> type) throws ClassNotFoundException {
        Class<?> cls = Class.forName(className);
        if (type.isAssignableFrom(cls)) {
            return (Class<T>) cls;
        }
        
        throw new IllegalArgumentException("Found " + className + " in the classpath, but it isn't of type " + type);
    }

    private static <T> T newInstance(Class<T> cls)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
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
}
