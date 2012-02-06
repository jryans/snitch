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
package com.bazaarvoice.snitch.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The default configuration supported by Snitch.  By default Snitch will look in the classpath to find configuration
 * settings.  It looks in <tt></tt></tT>META-INF/snitch.properties</tt> to find the specific application configuration
 * settings.  The covers things like which annotation to use to mark fields/methods that should be monitored, what
 * naming strategy to use, etc.  It will also look for all <tt>META-INF/snitch-formatters.properties</tt> files in the
 * classpath for formatter definitions.  This allows users of interesting libraries to provide Snitch compatible
 * formatters for the objects contained in those libraries without actually having the need to modify the source to
 * them.
 *
 * <table>
 *     <th>
 *         <tr><td colspan="2"><tt>META-INF/snitch.properties</tt> file format:</td></tr>
 *         <tr><td><b>Key</b></td><td><b>Description</b></td></tr>
 *     </th>
 *     <tr><td>annotation-class</td><td>Fully qualified class name to the annotation class to use</td></tr>
 *     <tr><td>naming-strategy-class</td><td>Fully qualified class name of the naming strategy class to use</td></tr>
 *     <tr><td>default-formatter-class</td><td>Fully qualified name of the default formatter to use</td></tr>
 *     <tr><td>packages</td><td>Comma separated list of packages that should be scanned</td></tr>
 * </table>
 *
 * <table>
 *     <th>
 *         <tr><td colspan="2"><tt>META-INF/snitch-formatters.properties</tt> file format:</td></tr>
 *         <tr><td><b>Class to format</b></td><td><b>Formatter class</b></td></tr>
 *     </th>
 *     <tr>Fully qualified class name<td>Fully qualified class name of the formatter class to use</td></tr>
 * </table>
 */
public class DefaultConfiguration implements Configuration {
    private static final String CORE_CONFIGURATION_FILENAME = "META-INF/snitch.properties";
    private static final String FORMATTER_CONFIGURATION_FILENAME = "META-INF/snitch-formatters.properties";
    private static final Splitter COMMA_SEPARATOR = Splitter.on(',').omitEmptyStrings().trimResults();

    private final String _annotationClassName;
    private final String _namingStrategyClassName;
    private final String _defaultFormatterClassName;
    private final ImmutableList<String> _packagesToScan;
    private final ImmutableMap<String, String> _formatterClassNames;

    public DefaultConfiguration() throws IOException {
        this(getPropertiesResource(CORE_CONFIGURATION_FILENAME),
             getPropertiesResources(FORMATTER_CONFIGURATION_FILENAME));
    }

    @VisibleForTesting
    DefaultConfiguration(Properties coreProperties, List<Properties> formatterProperties) throws IOException {
        _annotationClassName = coreProperties.getProperty("annotation-class");
        _namingStrategyClassName = coreProperties.getProperty("naming-strategy-class");
        _defaultFormatterClassName = coreProperties.getProperty("default-formatter-class");

        String packages = coreProperties.getProperty("packages");
        _packagesToScan = (packages != null)
                ? ImmutableList.<String>builder().addAll(COMMA_SEPARATOR.split(packages)).build()
                : ImmutableList.<String>of();

        Map<String, String> formatterMap = Maps.newHashMap();
        for (Properties formatterProps : formatterProperties) {
            for (String key : formatterProps.stringPropertyNames()) {
                formatterMap.put(key, formatterProps.getProperty(key));
            }
        }
        _formatterClassNames = ImmutableMap.copyOf(formatterMap);
    }

    @Override
    public String getAnnotationClassName() {
        return _annotationClassName;
    }

    @Override
    public String getNamingStrategyClassName() {
        return _namingStrategyClassName;
    }

    @Override
    public String getDefaultFormatterClassName() {
        return _defaultFormatterClassName;
    }

    @Override
    public List<String> getPackagesToScan() {
        return _packagesToScan;
    }

    @Override
    public Map<String, String> getFormatterClassNames() {
        return _formatterClassNames;
    }

    private static Properties getPropertiesResource(String name) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return loadProperties(loader.getResource(name));
    }

    private static List<Properties> getPropertiesResources(String name) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> urls = loader.getResources(name);

        List<Properties> props = Lists.newArrayList();
        while (urls.hasMoreElements()) {
            props.add(loadProperties(urls.nextElement()));
        }
        return props;
    }

    private static Properties loadProperties(URL url) throws IOException {
        Properties props = new Properties();
        if (url != null) {
            InputStream in = url.openStream();
            try {
                props.load(in);
            } finally {
                Closeables.closeQuietly(in);
            }
        }

        return props;
    }
}
