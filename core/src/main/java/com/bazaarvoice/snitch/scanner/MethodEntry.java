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

public class MethodEntry implements AnnotationScanner.MethodEntry {
    private final String _className;
    private final String _methodName;

    @VisibleForTesting
    MethodEntry(String className, String methodName) {
        _className = className;
        _methodName = methodName;
    }

    public String getClassName() {
        return _className;
    }

    public String getMethodName() {
        return _methodName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_className, _methodName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MethodEntry)) {
            return false;
        }

        MethodEntry that = (MethodEntry) obj;
        return Objects.equal(_className, that._className) &&
                Objects.equal(_methodName, that._methodName);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("className", _className)
                .add("methodName", _methodName)
                .toString();
    }
}
