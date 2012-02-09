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

import com.google.gson.stream.JsonWriter;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultFormatterTest {
    private final DefaultFormatter _formatter = DefaultFormatter.INSTANCE;
    private final JsonWriter _writer = mock(JsonWriter.class);

    @Test
    public void testNull() throws IOException {
        _formatter.format(null, _writer);
        verify(_writer).nullValue();
    }

    @Test
    public void testInteger() throws IOException {
        int num = 1;
        _formatter.format(num, _writer);
        verify(_writer).value(Integer.valueOf(num));
    }

    @Test
    public void testDouble() throws IOException {
        double num = 1.;
        _formatter.format(num, _writer);
        verify(_writer).value(Double.valueOf(num));
    }

    @Test
    public void testString() throws IOException {
        _formatter.format("string", _writer);
        verify(_writer).value("string");
    }

    @Test
    public void testBoolean() throws IOException {
        _formatter.format(true, _writer);
        verify(_writer).value(Boolean.TRUE);
    }

    @Test
    public void testObject() throws IOException {
        MockObject obj = new MockObject();
        _formatter.format(obj, _writer);
        assertTrue(obj._toStringCalled);
    }

    // We have to build our own mock, since Mockito won't allow us to verify toString().
    private static final class MockObject {
        private boolean _toStringCalled = false;

        @Override
        public String toString() {
            _toStringCalled = true;
            return "";
        }
    }
}
