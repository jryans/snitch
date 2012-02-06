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
package com.bazaarvoice.snitch.scanner;

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

public class ClassPathAnnotationScanner implements AnnotationScanner {
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
        if (packageNames.length == 0) {
            // As a heuristic load scan all of the packages that have had classes loaded already...
            Package[] packages = Package.getPackages();
            
            packageNames = new String[packages.length];
            for(int i = 0; i < packages.length; i++) {
                packageNames[i] = packages[i].getName();
            }
        }
        
        if (packageNames.length == 0) {
            // All attempts to find a reasonable set of packages have failed, use some common ones as a best attempt to
            // make Snitch work in most environments.
            packageNames = new String[] { "com", "org", "edu", "net" };
        }
        
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
            _typeAnnotations.put(annotation, new com.bazaarvoice.snitch.scanner.ClassEntry(clsName));
        }

        @Override
        public void reportMethodAnnotation(Class<? extends Annotation> annotation, String clsName, String methodName) {
            _methodAnnotations.put(annotation, new com.bazaarvoice.snitch.scanner.MethodEntry(clsName, methodName));
        }

        @Override
        public void reportFieldAnnotation(Class<? extends Annotation> annotation, String clsName, String fieldName) {
            _fieldAnnotations.put(annotation, new com.bazaarvoice.snitch.scanner.FieldEntry(clsName, fieldName));
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
}
