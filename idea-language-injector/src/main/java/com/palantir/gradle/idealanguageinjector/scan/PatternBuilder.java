/*
 * (c) Copyright 2025 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.gradle.idealanguageinjector.scan;

import java.util.List;
import java.util.stream.Collectors;

final class PatternBuilder {

    private PatternBuilder() {}

    static String buildDisplayName(String className) {
        String normalizedClassName = className.replace('$', '.');
        int lastDot = normalizedClassName.lastIndexOf('.');

        String simpleClassName = normalizedClassName.substring(lastDot + 1);
        String containingPackage = lastDot > 0 ? normalizedClassName.substring(0, lastDot) : "";

        return String.format("%s (%s)", simpleClassName, containingPackage);
    }

    static String buildPatternString(
            String className, String methodName, List<String> parameterTypes, int parameterIndex) {
        String paramTypes =
                parameterTypes.stream().map(type -> "\"" + type + "\"").collect(Collectors.joining(", "));

        String patternMethodName = methodName.equals("<init>") ? extractSimpleClassName(className) : methodName;

        String normalizedClassName = className.replace('$', '.');

        return String.format(
                "psiParameter().ofMethod(%d, psiMethod().withName(\"%s\").withParameters(%s).definedInClass(\"%s\"))",
                parameterIndex, patternMethodName, paramTypes, normalizedClassName);
    }

    private static String extractSimpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        int lastDollar = className.lastIndexOf('$');
        int lastDelimiter = Math.max(lastDot, lastDollar);
        return className.substring(lastDelimiter + 1);
    }
}
