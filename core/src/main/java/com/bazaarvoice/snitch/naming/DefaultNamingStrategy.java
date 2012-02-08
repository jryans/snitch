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
package com.bazaarvoice.snitch.naming;

import com.bazaarvoice.snitch.Monitored;
import com.google.common.base.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Default naming strategy that attempts to use the {@link com.bazaarvoice.snitch.Monitored} annotation if it's available, otherwise it
 * performs the following basic transformations:
 * <ul>
 *     <li>field names are not converted</li>
 *     <li>method names have javabean prefixes removed (e.g. {@code getMyData} is transformed to {@code myData}</li>
 * </ul>
 */
public class DefaultNamingStrategy implements NamingStrategy {
    public static final DefaultNamingStrategy INSTANCE = new DefaultNamingStrategy();

    // Singleton
    private DefaultNamingStrategy() {}

    @Override
    public String getName(Field field, Annotation annotation) {
        if (annotation instanceof Monitored) {
            String name = ((Monitored) annotation).value();
            if (!Strings.isNullOrEmpty(name)) {
                return name;
            }
        }

        return field.getName();
    }

    @Override
    public String getName(Method method, Annotation annotation) {
        if (annotation instanceof Monitored) {
            String name = ((Monitored) annotation).value();
            if (!Strings.isNullOrEmpty(name)) {
                return name;
            }
        }

        String name = method.getName();
        if (name.startsWith("get")) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }

        if (name.startsWith("is")) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }

        return name;
    }
}
