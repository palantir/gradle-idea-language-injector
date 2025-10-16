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

package com.palantir.gradle.idealanguageinjector.ideaxml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import java.util.List;
import java.util.stream.Collectors;
import org.immutables.value.Value;

/**
 * Place element containing PSI pattern for IntelliLang.xml.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutablePlace.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Place {

    @JacksonXmlText(value = true)
    @JacksonXmlCData
    String pattern();

    static Place of(String pattern) {
        return ImmutablePlace.builder().pattern(pattern).build();
    }

    static Place from(String className, String methodName, List<String> parameterTypes, int parameterIndex) {
        return ImmutablePlace.builder()
                .pattern(buildPatternString(className, methodName, parameterTypes, parameterIndex))
                .build();
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
