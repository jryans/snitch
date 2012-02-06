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

import com.bazaarvoice.snitch.Formatter;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/** The default Snitch formatter. */
public class DefaultFormatter implements Formatter<Object> {
    public static final DefaultFormatter INSTANCE = new DefaultFormatter();

    // Singleton
    private DefaultFormatter() {}

    @Override
    public void format(Object obj, JsonWriter writer) throws IOException {
        if (obj == null) {
            writer.nullValue();
        } else if (obj instanceof Number) {
            writer.value((Number) obj);
        } else if (obj instanceof String) {
            writer.value((String) obj);
        } else if (obj instanceof Boolean) {
            writer.value((Boolean) obj);
        } else {
            writer.value(obj.toString());
        }
    }
}
