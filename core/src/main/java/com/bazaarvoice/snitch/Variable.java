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
package com.bazaarvoice.snitch;

public interface Variable {
    /** The class that contained the annotated entity that this variable was created for. */
    Class<?> getOwner();

    /** The name of this variable. */
    String getName();

    /** The instance this variable is associated with or {@code null} if there is no instance. */
    Object getInstance();

    /** The type of the variable. */
    Class<?> getType();

    /** The value of the variable. */
    Object getValue();
}
