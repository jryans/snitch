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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Closeables;
import com.google.gson.stream.JsonWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * Servlet implementation for Snitch that allows the monitored variables to be accessed.  This implementation will emit
 * variables in JSON.
 */
public class VariableServlet extends HttpServlet {
    private static final long serialVersionUID = 0L;
    private final Snitch _snitch;

    public VariableServlet() {
        _snitch = Snitch.getInstance();
    }

    @VisibleForTesting
    VariableServlet(Snitch snitch) {
        _snitch = snitch;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        addClientNoCacheHeaders(response);
        response.setContentType("application/json");

        // Organize the variables into a multimap indexed by key
        Multimap<String, Variable> variables = HashMultimap.create();
        for (Variable variable : _snitch.getVariables()) {
            variables.put(variable.getName(), variable);
        }

        JsonWriter writer = new JsonWriter(new BufferedWriter(response.getWriter()));
        writer.setIndent("  ");  // Pretty print by default

        try {
            writer.beginObject();
            for (String name : variables.keySet()) {
                Collection<Variable> vars = variables.get(name);

                writer.name(name);
                if (vars.size() > 1) {
                    // Only render as an array if we have a name collision
                    writer.beginArray();
                }
                for (Variable variable : vars) {
                    Formatter formatter = _snitch.getFormatter(variable);
                    formatter.format(variable.getValue(), writer);
                }
                if (vars.size() > 1) {
                    writer.endArray();
                }
            }
            writer.endObject();
        } finally {
            Closeables.closeQuietly(writer);
        }
    }

    /**
     * Adds headers to the response which will keep the end-user's browser from caching the response.  Since these
     * values can update each time the servlet is invoked we don't want the browser or query tool to cache values.
     *
     * @see <a href="http://www.mnot.net/cache_docs">"Caching Tutorial"</a> for an explanation of all the cache
     *      control headers
     */
    private void addClientNoCacheHeaders(HttpServletResponse response) {
        // HTTP 1.0 header
        response.setDateHeader("Expires", 1L);

        // HTTP 1.1 headers: "no-cache" is the standard value,
        // "no-store" is necessary to prevent caching on FireFox.
        response.setHeader("Cache-Control", "no-cache, no-store");
    }
}
