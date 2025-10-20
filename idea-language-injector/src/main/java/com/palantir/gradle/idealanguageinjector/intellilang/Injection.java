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

package com.palantir.gradle.idealanguageinjector.intellilang;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

/**
 * Represents a single language injection rule in IntelliLang.xml.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableInjection.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface Injection {

    @JacksonXmlProperty(isAttribute = true)
    String language();

    @JacksonXmlProperty(isAttribute = true, localName = "injector-id")
    @Value.Default
    default String injectorId() {
        return "java";
    }

    @JacksonXmlProperty(localName = "display-name")
    String displayName();

    @JacksonXmlProperty(localName = "single-file")
    @Value.Default
    default SingleFile singleFile() {
        return SingleFile.defaultSingleFile();
    }

    @JacksonXmlProperty(localName = "place")
    @JacksonXmlElementWrapper(useWrapping = false)
    List<Place> places();

    static ImmutableInjection.Builder builder() {
        return ImmutableInjection.builder();
    }

    static Injection from(
            String className, String methodName, List<String> parameterTypes, int parameterIndex, String language) {
        return builder()
                .language(language)
                .displayName(buildDisplayName(className))
                .addPlaces(Place.from(className, methodName, parameterTypes, parameterIndex))
                .build();
    }

    static String buildDisplayName(String className) {
        String normalizedClassName = className.replace('$', '.');
        int lastDot = normalizedClassName.lastIndexOf('.');

        String simpleClassName = normalizedClassName.substring(lastDot + 1);
        String containingPackage = lastDot > 0 ? normalizedClassName.substring(0, lastDot) : "";

        return String.format("%s (%s)", simpleClassName, containingPackage);
    }

    /**
     * Merges multiple injections, combining injections with the same key (displayName, language, injectorId).
     */
    static List<Injection> mergeAll(List<Injection> injections) {
        Map<String, Injection> mergedMap = injections.stream()
                .collect(Collectors.toMap(
                        i -> i.displayName() + "|" + i.language() + "|" + i.injectorId(),
                        injection -> injection,
                        Injection::mergeWith,
                        LinkedHashMap::new));

        return mergedMap.values().stream()
                .sorted(Comparator.comparing(Injection::displayName)
                        .thenComparing(Injection::language)
                        .thenComparing(Injection::injectorId))
                .toList();
    }

    /**
     * Merges two injections by combining their places.
     */
    default Injection mergeWith(Injection other) {
        List<String> mergedPlaces = Stream.concat(this.places().stream(), other.places().stream())
                .map(Place::pattern)
                .distinct()
                .sorted()
                .toList();

        return builder()
                .from(this)
                .places(mergedPlaces.stream().map(Place::of).toList())
                .build();
    }
}
