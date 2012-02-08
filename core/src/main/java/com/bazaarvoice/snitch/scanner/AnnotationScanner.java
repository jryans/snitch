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

import java.lang.annotation.Annotation;
import java.util.List;

public interface AnnotationScanner {
    /** Add an annotation to scan for. */
    void addAnnotationClass(Class<? extends Annotation> annotationClass);

    /** Retrieve the classes that have been annotated with a specific annotation. */
    List<ClassEntry> getClassesAnnotatedWith(Class<? extends Annotation> annotationClass);

    /** Retrieve the methods that have been annotated with a specific annotation. */
    List<MethodEntry> getMethodsAnnotatedWith(Class<? extends Annotation> annotationClass);

    /** Retrieve the fields that have been annotated with a specific annotation. */
    List<FieldEntry> getFieldsAnnotatedWith(Class<? extends Annotation> annotationClass);

    /** Entry representing a class that has been annotated with a specific annotation. */
    interface ClassEntry {
        String getClassName();
    }

    /** Entry representing a method that has been annotated with a specific annotation. */
    interface MethodEntry {
        String getClassName();
        String getMethodName();
    }

    /** Entry representing a field that has been annotated with a specific annotation. */
    interface FieldEntry {
        String getClassName();
        String getFieldName();
    }
}
