/*
 * Copyright (c) 2012 Bazaarvoice
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bazaarvoice.snitch;

import java.util.Collection;

/**
 * Monitor that contains a set of variables and their formatters that can be used for system or application monitoring purposes.
 */
public interface Monitor {

    /**
     * Get the variables of any currently loaded classes.
     */
    Collection<Variable<?>> getVariables();

    /**
     * Register the given formatter for a given variable data type.
     *
     * @return The previous formatter, if any
     */
    <T> Formatter<? super T> registerFormatter(Class<T> type, Formatter<? super T> formatter, boolean override);

    /**
     * Retrieve the formatter for the given variable's data type.
     */
    <T> Formatter<? super T> getFormatter(Variable<T> variable);

    /**
     * Retrieve the formatter for the given data type.
     */
    <T> Formatter<? super T> getFormatter(Class<T> type);

}
