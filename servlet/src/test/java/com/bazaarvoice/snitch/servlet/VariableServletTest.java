/*
 * Copyright 2011 Bazaarvoice
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

import com.bazaarvoice.snitch.AnnotationMonitor;
import com.bazaarvoice.snitch.ToStringFormatter;
import com.bazaarvoice.snitch.Variable;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VariableServletTest {
    /** The registered variables for a test. */
    private List<Variable> _variables;

    /** The writer that the servlet writes its output to. */
    private StringWriter _output;

    private VariableServlet _servlet;
    private HttpServletRequest _request;
    private HttpServletResponse _response;

    @Before
    public void setup() throws ServletException, IOException {
        _variables = Lists.newArrayList();
        _output = new StringWriter();

        AnnotationMonitor monitor = mock(AnnotationMonitor.class);
        when(monitor.getFormatter(Matchers.<Class<Object>>any())).thenReturn(ToStringFormatter.INSTANCE);
        when(monitor.getVariables(any(ClassLoader.class))).thenReturn(_variables);

        ServletContext context = mock(ServletContext.class);
        when(context.getAttribute(VariableServlet.ANNOTATION_MONITOR_ATTRIBUTE_NAME)).thenReturn(monitor);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(context);

        _servlet = new VariableServlet();
        _servlet.init(config);

        _request = mock(HttpServletRequest.class);
        _response = mock(HttpServletResponse.class);
        when(_response.getWriter()).thenReturn(new PrintWriter(_output));
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
        Variable v = defineVariable("foo", 1);

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testStringVariable() throws IOException, ServletException {
        Variable v = defineVariable("foo", "bar");

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testBooleanVariable() throws IOException, ServletException {
        Variable v = defineVariable("foo", true);

        _servlet.doGet(_request, _response);
        assertVariablesInJson(_output.toString(), v);
    }

    @Test
    public void testNullVariable() throws IOException, ServletException {
        Variable v = defineVariable("foo", null, String.class);

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

    private <T> Variable defineVariable(String name, final T value) {
        return defineVariable(name, value, value.getClass());
    }

    private <T> Variable defineVariable(String name, T value, final Class<?> type) {
        Variable v = mock(Variable.class);
        when(v.getName()).thenReturn(name);
        when(v.getValue()).thenReturn(value);

        // For some reason the type inference doesn't work properly if we call thenReturn, so we use thenAnswer
        // instead and always return the class value (e.g. T).
        when(v.getType()).thenAnswer(new Answer<Class<?>>() {
            @Override
            public Class<?> answer(InvocationOnMock invocation) throws Throwable {
                return type;
            }
        });

        _variables.add(v);
        return v;
    }

    private void assertVariablesInJson(String json, Variable... vars) {
        Map m = new Gson().fromJson(json, Map.class);

        for (Variable v : vars) {
            Object actualValue = m.get(v.getName());
            assertVariableValueEquals(v, actualValue);
        }
    }

    private void assertVariableValueEquals(Variable expected, Object actual) {
        // Json primitives are only strings, numbers (long and double), booleans, and nulls
        Object value = expected.getValue();
        Class type = expected.getType();

        if (value == null) {
            assertEquals(value, actual);
        } else if (byte.class.equals(type) || Byte.class.equals(type)) {
            assertEquals(value, ((Number) actual).byteValue());
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            assertEquals(value, ((Number) actual).doubleValue());
        } else if (float.class.equals(type) || Float.class.equals(type)) {
            assertEquals(value, ((Number) actual).floatValue());
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            assertEquals(value, ((Number) actual).intValue());
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            assertEquals(value, ((Number) actual).longValue());
        } else if (short.class.equals(type) || Short.class.equals(type)) {
            assertEquals(value, ((Number) actual).shortValue());
        } else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
            assertEquals(value, actual);
        } else if (String.class.equals(type)) {
            assertEquals(value, actual);
        } else {
            fail();
        }
    }
}
