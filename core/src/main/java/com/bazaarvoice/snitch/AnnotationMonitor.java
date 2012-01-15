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

package com.bazaarvoice.snitch;

import com.bazaarvoice.snitch.formatters.ToStringFormatter;
import com.bazaarvoice.snitch.naming.DefaultNamingStrategy;
import com.bazaarvoice.snitch.processors.FormatterAnnotationProcessor;
import com.bazaarvoice.snitch.processors.VariableAnnotationProcessor;
import com.google.common.collect.MapMaker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;

/**
 * Monitor that reads class annotations to determine variables and formatters.
 */
public class AnnotationMonitor implements Monitor {

    private final ClassLoaderHelper _classLoaderHelper;
    private final ClassPathAnnotationScanner _scanner;
    private final VariableAnnotationProcessor _variableAnnotationProcessor = new VariableAnnotationProcessor(new DefaultNamingStrategy());
    private final FormatterAnnotationProcessor _formatterAnnotationProcessor = new FormatterAnnotationProcessor();

    private final ConcurrentMap<ClassPathAnnotationScanner.ClassMemberEntry, Variable<?>> _variables = new MapMaker().makeMap();
    private final ConcurrentMap<String, Formatter<?>> _formattersByType = new MapMaker().makeMap();

    public AnnotationMonitor(ClassLoader loader, String... packageNames) {
        _classLoaderHelper = new ClassLoaderHelper(loader);

        _scanner = new ClassPathAnnotationScanner(packageNames);
        _scanner.addAnnotationClass(Monitored.class);
        _scanner.addAnnotationClass(FormattedBy.class);

        processClasses(loader);
    }

    @Override
    public Collection<Variable<?>> getVariables() {
        return Collections.unmodifiableCollection(_variables.values());
    }

    private <T> void registerDefaultFormatter(Class<T> cls) {
        Formatter<? super T> formatter = _formatterAnnotationProcessor.createFormatter(cls);
        if (formatter != null) {
            registerFormatter(cls, formatter, false);
        }
    }

    @Override
    public <T> Formatter<? super T> registerFormatter(Class<T> type, Formatter<? super T> formatter, boolean override) {
        Formatter<? super T> previous;
        if (override) {
            //noinspection unchecked
            previous = (Formatter<? super T>) _formattersByType.put(type.getName(), formatter);
        } else {
            //noinspection unchecked
            previous = (Formatter<? super T>) _formattersByType.putIfAbsent(type.getName(), formatter);
        }
        return previous;
    }

    @Override
    public <T> Formatter<? super T> getFormatter(Variable<T> variable) {
        return getFormatter(variable.getType());
    }

    @Override
    public <T> Formatter<? super T> getFormatter(Class<T> type) {
        @SuppressWarnings ({"unchecked"})
        Formatter<? super T> formatter = (Formatter<? super T>) _formattersByType.get(type.getName());
        if (formatter != null) {
            return formatter;
        }

        return ToStringFormatter.INSTANCE;
    }

    private void processClasses(ClassLoader loader) {
        for (final ClassPathAnnotationScanner.FieldEntry fieldEntry : _scanner.getFieldsAnnotatedWith(Monitored.class)) {
            if (_variables.containsKey(fieldEntry)) {
                continue;
            }

            Class<?> cls = _classLoaderHelper.findLoadedClass(fieldEntry.getClassName());
            if (cls != null) {
                _variables.put(fieldEntry, _variableAnnotationProcessor.createFieldVariable(cls, fieldEntry.getFieldName()));
            }
        }

        for (final ClassPathAnnotationScanner.MethodEntry methodEntry : _scanner.getMethodsAnnotatedWith(Monitored.class)) {
            if (_variables.containsKey(methodEntry)) {
                continue;
            }

            Class<?> cls = _classLoaderHelper.findLoadedClass(methodEntry.getClassName());
            if (cls != null) {
                _variables.put(methodEntry, _variableAnnotationProcessor.createMethodVariable(cls, methodEntry.getMethodName()));
            }
        }

        for (final ClassPathAnnotationScanner.ClassEntry classEntry : _scanner.getClassesAnnotatedWith(FormattedBy.class)) {
            if (_formattersByType.containsKey(classEntry.getClassName())) {
                continue;
            }

            Class<?> cls = _classLoaderHelper.findLoadedClass(classEntry.getClassName());
            if (cls != null) {
                registerDefaultFormatter(cls);
            }
        }
    }
}
