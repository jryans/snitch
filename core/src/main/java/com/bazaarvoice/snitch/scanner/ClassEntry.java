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
package com.bazaarvoice.snitch.scanner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;

public class ClassEntry implements AnnotationScanner.ClassEntry {
    private final String _className;

    @VisibleForTesting
    ClassEntry(String className) {
        _className = className;
    }

    public String getClassName() {
        return _className;
    }

    @Override
    public int hashCode() {
        return _className.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClassEntry)) {
            return false;
        }

        ClassEntry that = (ClassEntry) obj;
        return Objects.equal(_className, that._className);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("className", _className)
                .toString();
    }
}
