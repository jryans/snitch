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
import com.bazaarvoice.snitch.Formatter;
import com.bazaarvoice.snitch.MonitoringAgent;
import com.bazaarvoice.snitch.Variable;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.gson.stream.JsonWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class VariableServlet extends HttpServlet {
    private static final long serialVersionUID = 0L;

    private AnnotationMonitor _monitor;

    @VisibleForTesting
    void setMonitor(AnnotationMonitor monitor) {
        _monitor = monitor;
    }

    @Override
    public void init() throws ServletException {
        if (_monitor == null) {
            _monitor = MonitoringAgent.getMonitor();
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        addClientNoCacheHeaders(response);

        JsonWriter writer = new JsonWriter(new BufferedWriter(response.getWriter()));
        writer.beginObject();
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            List<Variable> variables = _monitor.getVariables(loader);
            for (Variable variable : variables) {
                Formatter formatter = _monitor.getFormatter(variable.getType());
                Object value = variable.getValue();

                writer.name(variable.getName());
                formatter.format(value, writer);
            }
        } finally {
            writer.endObject();
            Closeables.closeQuietly(writer);
        }
    }

    /**
     * Adds headers to the response which will keep the end-user's browser from caching the response.  Since these
     * values can update each time the servlet is invoked we don't want the browser or query tool to cache values.
     *
     * @see <a href="http://www.mnot.net/cache_docs">"Caching Tutorial"</a> for an explanation of all the cache
     * control headers
     */
    private void addClientNoCacheHeaders(HttpServletResponse response) {
        // HTTP 1.0 header
        response.setDateHeader("Expires", 1L);

        // HTTP 1.1 headers: "no-cache" is the standard value,
        // "no-store" is necessary to prevent caching on FireFox.
        response.setHeader("Cache-Control", "no-cache, no-store");
    }
}
