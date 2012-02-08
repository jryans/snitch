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
package com.bazaarvoice.snitch.formatters;

import com.bazaarvoice.snitch.FormattedBy;
import com.bazaarvoice.snitch.Formatter;
import com.google.gson.stream.JsonWriter;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FormatterRegistryTest {
    private final FormatterRegistry _registry = new FormatterRegistry(DefaultFormatter.INSTANCE);

    @Test
    public void testDefaultFormatter() {
        assertEquals(DefaultFormatter.INSTANCE, _registry.getFormatter(Object.class));
    }

    @Test
    public void testDefaultFormatterOverride() {
        Formatter<?> defaultFormatter = new Formatter<Object>() {
            @Override
            public void format(Object obj, JsonWriter writer) throws IOException {
            }
        };

        _registry.setDefaultFormatter(defaultFormatter);
        assertEquals(defaultFormatter, _registry.getFormatter(Object.class));
    }
    
    @Test
    public void testFormatterOverride() {
        Formatter<Object> formatter = new Formatter<Object>() {
            @Override
            public void format(Object obj, JsonWriter writer) throws IOException {
            }
        };
        
        _registry.registerFormatter(Object.class, formatter);
        assertEquals(formatter, _registry.getFormatter(Object.class));
    }
    
    @Test
    public void testFormattedByAnnotation() {
        Formatter<TestObject> formatter = _registry.getFormatter(TestObject.class);
        assertTrue(formatter instanceof TestFormatter);
    }

    @FormattedBy(TestFormatter.class)
    private static final class TestObject {
    }

    private static final class TestFormatter implements Formatter<TestObject> {
        @Override
        public void format(TestObject obj, JsonWriter writer) throws IOException {
        }
    }
}
