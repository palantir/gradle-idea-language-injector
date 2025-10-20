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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ComponentMergeTest {

    @Test
    void component_of_automatically_merges_injections() {
        IntelliLangInjection injection1 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();

        IntelliLangInjection injection2 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build();

        IntelliLangComponent expected = IntelliLangComponent.of(List.of(IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build()));
        IntelliLangComponent actual = IntelliLangComponent.of(List.of(injection1, injection2));

        assertThat(actual)
                .as("component should automatically merge injections with same key")
                .isEqualTo(expected);
    }

    @Test
    void component_of_with_multiple_different_injections() {
        IntelliLangInjection injection1 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass1 (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();

        IntelliLangInjection injection2 = IntelliLangInjection.builder()
                .language("JSON")
                .displayName("TestClass2 (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build();

        IntelliLangInjection injection3 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass1 (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern3"))
                .build();

        IntelliLangComponent expected = IntelliLangComponent.of(List.of(
                IntelliLangInjection.builder()
                        .language("JSON")
                        .displayName("TestClass2 (com.example)")
                        .addPlaces(IntelliLangPlace.of("pattern2"))
                        .build(),
                IntelliLangInjection.builder()
                        .language("SQL")
                        .displayName("TestClass1 (com.example)")
                        .addPlaces(IntelliLangPlace.of("pattern1"))
                        .addPlaces(IntelliLangPlace.of("pattern3"))
                        .build()));
        IntelliLangComponent actual = IntelliLangComponent.of(List.of(injection1, injection2, injection3));

        assertThat(actual)
                .as("component should merge injections with same key and keep different ones separate")
                .isEqualTo(expected);
    }

    @Test
    void component_of_with_empty_list() {
        IntelliLangComponent expected = IntelliLangComponent.of(List.of());
        IntelliLangComponent actual = IntelliLangComponent.of(List.of());

        assertThat(actual)
                .as("component with empty list should have no injections")
                .isEqualTo(expected);
    }

    @Test
    void component_name() {
        String expected = "LanguageInjectionConfiguration";
        String actual = IntelliLangComponent.of(List.of()).name();

        assertThat(actual)
                .as("component name should be LanguageInjectionConfiguration")
                .isEqualTo(expected);
    }
}
