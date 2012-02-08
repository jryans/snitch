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

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultConfigurationTest {
    private static final Properties EMPTY = new Properties();
    private static final List<Properties> EMPTY_LIST = Collections.emptyList();

    @Test
    public void testEmptyProperties() throws Exception {
        DefaultConfiguration config = new DefaultConfiguration(EMPTY, EMPTY_LIST);

        assertNull(config.getAnnotationClassName());
        assertNull(config.getDefaultFormatterClassName());
        assertNull(config.getNamingStrategyClassName());
        assertTrue(config.getFormatterClassNames().isEmpty());
        assertTrue(config.getPackagesToScan().isEmpty());
    }

    @Test
    public void testAnnotationClassName() throws Exception {
        String className = "class name";
        Properties props = props("annotation-class", className);
        DefaultConfiguration config = new DefaultConfiguration(props, EMPTY_LIST);

        assertEquals(className, config.getAnnotationClassName());
    }

    @Test
    public void testDefaultFormatterClassName() throws Exception {
        String className = "class name";
        Properties props = props("default-formatter-class", className);
        DefaultConfiguration config = new DefaultConfiguration(props, EMPTY_LIST);

        assertEquals(className, config.getDefaultFormatterClassName());
    }

    @Test
    public void testNamingStrategyClassName() throws Exception {
        String className = "class name";
        Properties props = props("naming-strategy-class", className);
        DefaultConfiguration config = new DefaultConfiguration(props, EMPTY_LIST);

        assertEquals(className, config.getNamingStrategyClassName());
    }

    @Test
    public void testOnePackageToScan() throws Exception {
        String packageName = "com.bazaarvoice";
        Properties props = props("packages", packageName);
        DefaultConfiguration config = new DefaultConfiguration(props, EMPTY_LIST);

        List<String> packages = config.getPackagesToScan();
        assertEquals(1, packages.size());
        assertTrue(packages.contains("com.bazaarvoice"));
    }

    @Test
    public void testMultiplePackagesToScan() throws Exception {
        String packageName = "com.bazaarvoice,com.google";
        Properties props = props("packages", packageName);
        DefaultConfiguration config = new DefaultConfiguration(props, EMPTY_LIST);

        List<String> packages = config.getPackagesToScan();
        assertEquals(2, packages.size());
        assertTrue(packages.contains("com.bazaarvoice"));
        assertTrue(packages.contains("com.google"));
    }

    @Test
    public void testMultiplePackagesToScanWithOddFormatting() throws Exception {
        String packageName = "  ,com.bazaarvoice,,\t\n,com.google,  ";
        Properties props = props("packages", packageName);
        DefaultConfiguration config = new DefaultConfiguration(props, EMPTY_LIST);

        List<String> packages = config.getPackagesToScan();
        assertEquals(2, packages.size());
        assertTrue(packages.contains("com.bazaarvoice"));
        assertTrue(packages.contains("com.google"));
    }

    @Test
    public void testFormatterClassNamesOneSource() throws Exception {
        Properties props = props("class", "formatter");
        DefaultConfiguration config = new DefaultConfiguration(EMPTY, Collections.singletonList(props));

        Map<String, String> formatters = config.getFormatterClassNames();
        assertEquals(1, formatters.size());
        assertEquals("formatter", formatters.get("class"));
    }

    @Test
    public void testMultipleFormatterClassNamesOneSource() throws Exception {
        Properties props = props("class1", "formatter1", "class2", "formatter2");
        DefaultConfiguration config = new DefaultConfiguration(EMPTY, Collections.singletonList(props));

        Map<String, String> formatters = config.getFormatterClassNames();
        assertEquals(2, formatters.size());
        assertEquals("formatter1", formatters.get("class1"));
        assertEquals("formatter2", formatters.get("class2"));
    }

    @Test
    public void testFormatterClassNamesMultipleSources() throws Exception {
        Properties props1 = props("class1", "formatter1");
        Properties props2 = props("class2", "formatter2");
        Properties props3 = props("class3", "formatter3");
        DefaultConfiguration config = new DefaultConfiguration(EMPTY, Lists.newArrayList(props1, props2, props3));

        Map<String, String> formatters = config.getFormatterClassNames();
        assertEquals(3, formatters.size());
        assertEquals("formatter1", formatters.get("class1"));
        assertEquals("formatter2", formatters.get("class2"));
        assertEquals("formatter3", formatters.get("class3"));
    }

    private static Properties props(String key, String value) {
        Properties props = new Properties();
        props.setProperty(key, value);
        return props;
    }

    private static Properties props(String key1, String value1, String key2, String value2) {
        Properties props = new Properties();
        props.setProperty(key1, value1);
        props.setProperty(key2, value2);
        return props;
    }
}
