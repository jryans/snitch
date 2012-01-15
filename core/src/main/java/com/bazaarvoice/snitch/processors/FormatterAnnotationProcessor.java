/*
 * Copyright (c) 2012 Bazaarvoice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bazaarvoice.snitch.processors;

import com.bazaarvoice.snitch.FormattedBy;
import com.bazaarvoice.snitch.Formatter;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;

public class FormatterAnnotationProcessor {
    public <T> Formatter<? super T> createFormatter(Class<T> cls) {
        // Check for the formatter annotation...
        FormattedBy annotation = cls.getAnnotation(FormattedBy.class);
        if (annotation == null) {
            return null;
        }

        //noinspection unchecked
        return newInstance(annotation.value());
    }

    private <T> T newInstance(Class<T> cls) {
        // Find the default constructor for the class
        try {
            Constructor<T> constructor = cls.getDeclaredConstructor();

            // Make sure we can call it
            constructor.setAccessible(true);

            return constructor.newInstance();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}