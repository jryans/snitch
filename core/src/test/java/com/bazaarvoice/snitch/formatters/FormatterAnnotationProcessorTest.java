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

package com.bazaarvoice.snitch.formatters;

import com.bazaarvoice.snitch.FormattedBy;
import com.bazaarvoice.snitch.Formatter;
import com.bazaarvoice.snitch.processors.FormatterAnnotationProcessor;
import com.google.gson.stream.JsonWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FormatterAnnotationProcessorTest {
    private FormatterAnnotationProcessor _processor;

    @Before
    public void setup() {
        _processor = new FormatterAnnotationProcessor();
    }

    @Test
    public void testFormatterWithDefaultConstructors() {
        @FormattedBy(FormatterWithSyntheticDefaultConstructor.class)
        class A implements Data {}

        @FormattedBy(FormatterWithPublicDefaultConstructor.class)
        class B implements Data {}

        @FormattedBy(FormatterWithPackageProtectedDefaultConstructor.class)
        class C implements Data {}

        @FormattedBy(FormatterWithProtectedDefaultConstructor.class)
        class D implements Data {}

        @FormattedBy(FormatterWithPrivateDefaultConstructor.class)
        class E implements Data {}

        assertEquals(FormatterWithSyntheticDefaultConstructor.class, createFormatter(A.class).getClass());
        assertEquals(FormatterWithPublicDefaultConstructor.class, createFormatter(B.class).getClass());
        assertEquals(FormatterWithPackageProtectedDefaultConstructor.class, createFormatter(C.class).getClass());
        assertEquals(FormatterWithProtectedDefaultConstructor.class, createFormatter(D.class).getClass());
        assertEquals(FormatterWithPrivateDefaultConstructor.class, createFormatter(E.class).getClass());
    }

    @Test
    public void testFormatterWithNonDefaultConstructor() {
        @FormattedBy (FormatterWithNonDefaultConstructor.class)
        class A implements Data {}

        try {
            createFormatter(A.class);
            fail();
        } catch (RuntimeException e) {
            assertEquals(NoSuchMethodException.class, e.getCause().getClass());
        }
    }

    private <T> Formatter<? super T> createFormatter(Class<T> cls) {
        // Make sure the processor processes this class before we try to load a formatter for it.
        return _processor.createFormatter(cls);
    }

    private static interface Data {}

    // Formatters

    private static class BaseFormatter implements Formatter<Data> {
        @Override
        public void format(Data obj, JsonWriter writer) throws IOException {}
    }

    private static final class FormatterWithSyntheticDefaultConstructor extends BaseFormatter {
    }

    private static final class FormatterWithPublicDefaultConstructor extends BaseFormatter {
        public FormatterWithPublicDefaultConstructor() {}
    }

    private static final class FormatterWithPackageProtectedDefaultConstructor extends BaseFormatter {
        FormatterWithPackageProtectedDefaultConstructor() {}
    }

    private static final class FormatterWithProtectedDefaultConstructor extends BaseFormatter {
        public FormatterWithProtectedDefaultConstructor() {}
    }

    private static final class FormatterWithPrivateDefaultConstructor extends BaseFormatter {
        public FormatterWithPrivateDefaultConstructor() {}
    }

    private static final class FormatterWithNonDefaultConstructor extends BaseFormatter {
        @SuppressWarnings({"UnusedParameters"})
        public FormatterWithNonDefaultConstructor(int i) {}
    }
}
