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

import java.util.List;
import java.util.Map;

public interface Configuration {
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Core Snitch properties, loaded from snitch.properties
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** The name of the annotation class that Snitch should scan the classpath for. */
    String getAnnotationClassName();

    /** The name of the class that Snitch should use for naming found variables. */
    String getNamingStrategyClassName();

    /** The name of the class that Snitch should use as the default variable formatter. */
    String getDefaultFormatterClassName();

    /** The names of the packages that Snitch should scan in the classpath. */
    List<String> getPackagesToScan();


    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Snitch formatter properties, loaded from snitch-formatters.properties (all present in the classpath)
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** A mapping from object class name to formatter class name. */
    Map<String, String> getFormatterClassNames();
}
