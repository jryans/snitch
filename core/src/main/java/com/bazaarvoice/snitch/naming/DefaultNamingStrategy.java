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

package com.bazaarvoice.snitch.naming;

import com.bazaarvoice.snitch.Monitored;
import com.bazaarvoice.snitch.NamingStrategy;
import com.google.common.base.Strings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Uses the annotation if available, otherwise uses the following defaults:
 * <ul>
 *     <li>fields are not converted</li>
 *     <li>methods are converted as follows: <tt>getMyData()</tt> converts to <tt>myData</tt></li>
 * </ul>
 */
public class DefaultNamingStrategy implements NamingStrategy {
    public String getName(Field field) {
        Monitored monitoredAnnotation = field.getAnnotation(Monitored.class);
        if (monitoredAnnotation != null) {
            String value = monitoredAnnotation.value();
            if (!Strings.isNullOrEmpty(value)) {
                return value;
            }
        }

        return field.getName();
    }

    public String getName(Method method) {
        Monitored monitorAnnotation = method.getAnnotation(Monitored.class);
        if (monitorAnnotation != null) {
            String value = monitorAnnotation.value();
            if (!Strings.isNullOrEmpty(value)) {
                return value;
            }
        }

        String methodName = method.getName();
        if (methodName.startsWith("get")) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }

        if (methodName.startsWith("is")) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }

        return methodName;
    }
}
