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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import eu.infomas.annotation.AnnotationDetector;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClassPathAnnotationScanner {
    private final String[] _packageNames;

    /** The set of annotations to scan for. */
    private final Set<Class<? extends Annotation>> _annotationClasses = Sets.newHashSet();

    /** Flag indicating that a class path scan is needed, because a new annotation has been added. */
    private volatile boolean _needsScan = false;

    /** The set of class entries that have been discovered from the class path */
    private volatile ListMultimap<Class<? extends Annotation>, ClassEntry> _classEntries = ImmutableListMultimap.of();

    /** The set of method entries that have been discovered from the class path */
    private volatile ListMultimap<Class<? extends Annotation>, MethodEntry> _methodEntries = ImmutableListMultimap.of();

    /** The set of field entries that have been discovered from the class path */
    private volatile ListMultimap<Class<? extends Annotation>, FieldEntry> _fieldEntries = ImmutableListMultimap.of();

    public ClassPathAnnotationScanner(String... packageNames) {
        _packageNames = Arrays.copyOf(packageNames, packageNames.length);
    }

    public synchronized void addAnnotationClass(Class<? extends Annotation> annotationClass) {
        if (_annotationClasses.add(annotationClass)) {
            _needsScan = true;
        }
    }

    public List<ClassEntry> getClassesAnnotatedWith(Class<? extends Annotation> annotationClass) {
        checkScan();
        return _classEntries.get(annotationClass);
    }

    public List<MethodEntry> getMethodsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        checkScan();
        return _methodEntries.get(annotationClass);
    }

    public List<FieldEntry> getFieldsAnnotatedWith(Class<? extends Annotation> annotationClass) {
        checkScan();
        return _fieldEntries.get(annotationClass);
    }

    private void checkScan() {
        if (_needsScan) {
            scanForAnnotations();
        }
    }

    private synchronized void scanForAnnotations() {
        if (!_needsScan) {
            return;
        }

        // Scan the classpath for annotations
        Reporter reporter = new Reporter(_annotationClasses);
        try {
            new AnnotationDetector(reporter).detect(_packageNames);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        // Remember the annotated classes, methods and fields for later
        _classEntries = reporter.getTypeAnnotations();
        _methodEntries = reporter.getMethodAnnotations();
        _fieldEntries = reporter.getFieldAnnotations();

        _needsScan = false;
    }

    private static final class Reporter implements AnnotationDetector.FieldReporter, AnnotationDetector.MethodReporter,
            AnnotationDetector.TypeReporter {
        private final Class<? extends Annotation>[] _annotations;
        private final ListMultimap<Class<? extends Annotation>, ClassEntry> _typeAnnotations;
        private final ListMultimap<Class<? extends Annotation>, MethodEntry> _methodAnnotations;
        private final ListMultimap<Class<? extends Annotation>, FieldEntry> _fieldAnnotations;

        @SuppressWarnings("unchecked")
        public Reporter(Set<Class<? extends Annotation>> annotations) {
            _annotations = annotations.toArray(new Class[annotations.size()]);
            _typeAnnotations = ArrayListMultimap.create();
            _methodAnnotations = ArrayListMultimap.create();
            _fieldAnnotations = ArrayListMultimap.create();
        }

        @Override
        public Class<? extends Annotation>[] annotations() {
            return _annotations;
        }

        @Override
        public void reportTypeAnnotation(Class<? extends Annotation> annotation, String clsName) {
            _typeAnnotations.put(annotation, new ClassEntry(clsName));
        }

        @Override
        public void reportMethodAnnotation(Class<? extends Annotation> annotation, String clsName, String methodName) {
            _methodAnnotations.put(annotation, new MethodEntry(clsName, methodName));
        }

        @Override
        public void reportFieldAnnotation(Class<? extends Annotation> annotation, String clsName, String fieldName) {
            _fieldAnnotations.put(annotation, new FieldEntry(clsName, fieldName));
        }

        public ListMultimap<Class<? extends Annotation>, ClassEntry> getTypeAnnotations() {
            return Multimaps.unmodifiableListMultimap(_typeAnnotations);
        }

        public ListMultimap<Class<? extends Annotation>, MethodEntry> getMethodAnnotations() {
            return Multimaps.unmodifiableListMultimap(_methodAnnotations);
        }

        public ListMultimap<Class<? extends Annotation>, FieldEntry> getFieldAnnotations() {
            return Multimaps.unmodifiableListMultimap(_fieldAnnotations);
        }
    }

    public static final class ClassEntry {
        private final String _className;

        @VisibleForTesting
        ClassEntry(String className) {
            _className = className;
        }

        public String getClassName() {
            return _className;
        }

        @Override
        public int hashCode() {
            return _className.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ClassEntry)) {
                return false;
            }

            ClassEntry that = (ClassEntry) obj;
            return Objects.equal(_className, that._className);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("className", _className)
                    .toString();
        }
    }

    public static final class MethodEntry {
        private final String _className;
        private final String _methodName;

        @VisibleForTesting
        MethodEntry(String className, String methodName) {
            _className = className;
            _methodName = methodName;
        }

        public String getClassName() {
            return _className;
        }

        public String getMethodName() {
            return _methodName;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(_className, _methodName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodEntry)) {
                return false;
            }

            MethodEntry that = (MethodEntry) obj;
            return Objects.equal(_className, that._className) &&
                    Objects.equal(_methodName, that._methodName);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("className", _className)
                    .add("methodName", _methodName)
                    .toString();
        }
    }

    public static final class FieldEntry {
        private final String _className;
        private final String _fieldName;

        @VisibleForTesting
        FieldEntry(String className, String fieldName) {
            _className = className;
            _fieldName = fieldName;
        }

        public String getClassName() {
            return _className;
        }

        public String getFieldName() {
            return _fieldName;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(_className, _fieldName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FieldEntry)) {
                return false;
            }

            FieldEntry that = (FieldEntry) obj;
            return Objects.equal(_className, that._className) &&
                    Objects.equal(_fieldName, that._fieldName);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("className", _className)
                    .add("fieldName", _fieldName)
                    .toString();
        }
    }
}
