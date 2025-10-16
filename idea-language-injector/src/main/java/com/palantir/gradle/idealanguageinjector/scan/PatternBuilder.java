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
        int lastPackageDot = className.lastIndexOf('.');
        String packageName = lastPackageDot > 0 ? className.substring(0, lastPackageDot) : "";
        String classHierarchy = className.substring(lastPackageDot + 1).replace('$', '.');

        int lastHierarchyDot = classHierarchy.lastIndexOf('.');
        String simpleClassName =
                lastHierarchyDot == -1 ? classHierarchy : classHierarchy.substring(lastHierarchyDot + 1);

        String containingPackage = lastHierarchyDot == -1
                ? packageName
                : (packageName.isEmpty()
                        ? classHierarchy.substring(0, lastHierarchyDot)
                        : packageName + "." + classHierarchy.substring(0, lastHierarchyDot));

        return String.format("%s (%s)", simpleClassName, containingPackage);
    }

    static String buildPatternString(
            String className, String methodName, List<String> parameterTypes, int parameterIndex) {
        String paramTypes =
                parameterTypes.stream().map(type -> "\"" + type + "\"").collect(Collectors.joining(", "));

        String patternMethodName = "<init>".equals(methodName) ? extractSimpleClassName(className) : methodName;

        String normalizedClassName = className.replace('$', '.');

        return String.format(
                "psiParameter().ofMethod(%d, psiMethod().withName(\"%s\").withParameters(%s).definedInClass(\"%s\"))",
                parameterIndex, patternMethodName, paramTypes, normalizedClassName);
    }

    private static String extractSimpleClassName(String className) {
        String internalName = className.replace('.', '/');
        int lastDelimiter = Math.max(internalName.lastIndexOf('$'), internalName.lastIndexOf('/'));
        return internalName.substring(lastDelimiter + 1);
    }
}
