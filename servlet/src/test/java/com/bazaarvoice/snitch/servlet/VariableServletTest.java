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
package com.bazaarvoice.snitch.servlet;

import com.bazaarvoice.snitch.Formatter;
import com.bazaarvoice.snitch.Snitch;
import com.bazaarvoice.snitch.Variable;
import com.bazaarvoice.snitch.formatters.DefaultFormatter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VariableServletTest {
    private final VariableServlet _servlet;

    // Mocks
    private final Snitch _snitch;
    private final HttpServletRequest _request;
    private final HttpServletResponse _response;

    // Helpers
    private final List<Variable> _variables;
    private final StringWriter _output;

    public VariableServletTest() throws IOException, ServletException {
        _variables = Lists.newArrayList();
        _output = new StringWriter();

        _snitch = mock(Snitch.class);
        when((Formatter) _snitch.getFormatter(Matchers.<Variable>any())).thenReturn(DefaultFormatter.INSTANCE);
        when(_snitch.getVariables()).thenReturn(_variables);

        _request = mock(HttpServletRequest.class);
        _response = mock(HttpServletResponse.class);
        when(_response.getWriter()).thenReturn(new PrintWriter(_output));

        _servlet = new VariableServlet(_snitch);
    }

    @Test
    public void testContentType() throws IOException, ServletException {
        _servlet.doGet(_request, _response);
        verify(_response).setContentType("application/json");
    }

    @Test
    public void testCacheHeaders() throws IOException, ServletException {
        _servlet.doGet(_request, _response);
        verify(_response).setDateHeader("Expires", 1L);
        verify(_response).setHeader("Cache-Control", "no-cache, no-store");
    }

    @Test
    public void testNoVariables() throws IOException, ServletException {
        _servlet.doGet(_request, _response);
        assertEquals("{}", _output.toString());
    }

    @Test
    public void testIntegerVariable() throws IOException, ServletException {
        Variable v = defineVariable("integer", 1);

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testStringVariable() throws IOException, ServletException {
        Variable v = defineVariable("string", "bar");

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testBooleanVariable() throws IOException, ServletException {
        Variable v = defineVariable("boolean", true);

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testNullVariable() throws IOException, ServletException {
        Variable v = defineVariable("string", null, String.class);

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testMultipleVariables() throws IOException, ServletException {
        Variable a = defineVariable("a", 1);
        Variable b = defineVariable("b", 10.);
        Variable c = defineVariable("c", "str");
        Variable d = defineVariable("d", false);
        Variable e = defineVariable("e", true);
        Variable f = defineVariable("f", null, Integer.class);

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), a, b, c, d, e, f);
    }

    @Test
    public void testDuplicateVariables() throws IOException, ServletException {
        String name = "a";
        Variable a1 = defineVariable(name, 1.0);
        Variable a2 = defineVariable(name, "second a");

        _servlet.doGet(_request, _response);

        Map<String, Object> jsonVarMap = parseJson(_output.toString());
        assertEquals(1, jsonVarMap.size());
        assertTrue(jsonVarMap.get(name) instanceof List);
        
        List jsonVars = (List) jsonVarMap.get(name);
        assertEquals(2, jsonVars.size());
        assertTrue(jsonVars.contains(a1.getValue()));
        assertTrue(jsonVars.contains(a2.getValue()));
    }

    @SuppressWarnings({"unchecked"})
    private <T> Variable defineVariable(String name, final T value) {
        return defineVariable(name, value, (Class<T>) value.getClass());
    }

    private <T> Variable defineVariable(String name, T value, final Class<T> type) {
        Variable v = mock(Variable.class);
        when(v.getName()).thenReturn(name);
        when(v.getValue()).thenReturn(value);

        // For some reason the type inference doesn't work properly if we call thenReturn, so we use thenAnswer
        // instead and always return the class value (e.g. T).
        when(v.getType()).thenAnswer(new Answer<Class<T>>() {
            @Override
            public Class<T> answer(InvocationOnMock invocation) throws Throwable {
                return type;
            }
        });

        _variables.add(v);
        return v;
    }

    @SuppressWarnings({"unchecked"})
    private Map<String, Object> parseJson(String json) {
        return new Gson().fromJson(json, Map.class);
    }

    private void assertVariablesInJson(String json, Variable... vars) {
        Map<String, Object> m = parseJson(json);
        for (Variable v : vars) {
            Class<?> type = v.getType();
            Object expectedValue = v.getValue();
            Object actualValue = m.get(v.getName());
            assertTypedEquals(type, expectedValue, actualValue);
        }
    }

    /**
     * Checks two variables for equality, but first converts {@code actual} from a Gson return type to the
     * same type as {@code expected}.
     */
    private void assertTypedEquals(Class<?> type, Object expected, Object actual) {
        // Json primitives are only strings, numbers (long and double), booleans, and nulls
        if (expected == null) {
            assertEquals(expected, actual);
        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            assertEquals(expected, actual);
        } else if (byte.class.equals(type) || Byte.class.equals(type)) {
            assertEquals(expected, ((Number) actual).byteValue());
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            assertEquals(expected, ((Number) actual).doubleValue());
        } else if (float.class.equals(type) || Float.class.equals(type)) {
            assertEquals(expected, ((Number) actual).floatValue());
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            assertEquals(expected, ((Number) actual).intValue());
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            assertEquals(expected, ((Number) actual).longValue());
        } else if (short.class.equals(type) || Short.class.equals(type)) {
            assertEquals(expected, ((Number) actual).shortValue());
        } else if (String.class.equals(type)) {
            assertEquals(expected, actual);
        } else {
            fail("Unrecognized type: " + type);
        }
    }
}
