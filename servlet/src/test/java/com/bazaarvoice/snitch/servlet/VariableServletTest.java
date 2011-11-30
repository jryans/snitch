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
import com.bazaarvoice.snitch.Variable;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VariableServletTest {
    private AnnotationMonitor _mockMonitor;
    private VariableServlet _servlet;

    @Before
    public void setup() throws ServletException {
        _mockMonitor = mock(AnnotationMonitor.class);

        ServletContext context = mock(ServletContext.class);
        when(context.getAttribute(VariableServlet.ANNOTATION_MONITOR_ATTRIBUTE_NAME)).thenReturn(_mockMonitor);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(context);

        _servlet = new VariableServlet();
        _servlet.init(config);
    }

    @Test
    public void test() {
        Variable v = mock(Variable.class);
        when(v.getType()).thenReturn(String.class);
        when(v.getValue()).thenReturn("v");
    }
}
