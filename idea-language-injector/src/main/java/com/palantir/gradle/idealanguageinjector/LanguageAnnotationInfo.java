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

package com.palantir.gradle.idealanguageinjector;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Information about a @Language annotation found on a method parameter.
 */
public record LanguageAnnotationInfo(
        String className, String methodName, int parameterIndex, String languageValue, List<String> parameterTypes) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void writeToFile(List<LanguageAnnotationInfo> annotations, File file) throws IOException {
        MAPPER.writeValue(file, annotations);
    }

    public static List<LanguageAnnotationInfo> readFromFile(File file) throws IOException {
        return MAPPER.readValue(
                file, MAPPER.getTypeFactory().constructCollectionType(List.class, LanguageAnnotationInfo.class));
    }

    public String pattern() {
        String paramTypes =
                parameterTypes().stream().map(type -> "\"" + type + "\"").collect(Collectors.joining(", "));

        return String.format(
                "psiParameter().ofMethod(%d, psiMethod().withName(\"%s\").withParameters(%s).definedInClass(\"%s\"))",
                parameterIndex(),
                getMethodNameForPattern(),
                paramTypes,
                className().replace('$', '.'));
    }

    public String displayName() {
        String fullClassName = className();
        int lastPackageDot = fullClassName.lastIndexOf('.');

        String packageName = lastPackageDot > 0 ? fullClassName.substring(0, lastPackageDot) : "";
        String classHierarchy = fullClassName.substring(lastPackageDot + 1).replace('$', '.');

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

    private String getMethodNameForPattern() {
        if (!methodName().equals("<init>")) {
            return methodName();
        }

        // For constructors, extract the simple class name
        String internalName = className().replace('.', '/');
        int lastDelimiter = Math.max(internalName.lastIndexOf('$'), internalName.lastIndexOf('/'));
        return internalName.substring(lastDelimiter + 1);
    }
}
