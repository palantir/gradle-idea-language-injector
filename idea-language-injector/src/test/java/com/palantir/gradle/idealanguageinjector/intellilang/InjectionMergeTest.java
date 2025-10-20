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

public class InjectionMergeTest {

    @Test
    void merge_with_combines_places() {
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

        IntelliLangInjection expected = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build();
        IntelliLangInjection actual = injection1.mergeWith(injection2);

        assertThat(actual).as("mergeWith should combine places from both injections").isEqualTo(expected);
    }

    @Test
    void merge_with_removes_duplicate_places() {
        IntelliLangInjection injection1 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build();

        IntelliLangInjection injection2 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .addPlaces(IntelliLangPlace.of("pattern3"))
                .build();

        IntelliLangInjection expected = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .addPlaces(IntelliLangPlace.of("pattern3"))
                .build();
        IntelliLangInjection actual = injection1.mergeWith(injection2);

        assertThat(actual).as("mergeWith should deduplicate places").isEqualTo(expected);
    }

    @Test
    void merge_all_combines_injections_with_same_key() {
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

        List<IntelliLangInjection> expected = List.of(IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build());
        List<IntelliLangInjection> actual = IntelliLangInjection.mergeAll(List.of(injection1, injection2));

        assertThat(actual).as("mergeAll should combine injections with same key").isEqualTo(expected);
    }

    @Test
    void merge_all_keeps_separate_injections_with_different_keys() {
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

        List<IntelliLangInjection> expected = List.of(injection2, injection1);
        List<IntelliLangInjection> actual = IntelliLangInjection.mergeAll(List.of(injection1, injection2));

        assertThat(actual)
                .as("mergeAll should keep injections with different keys separate")
                .isEqualTo(expected);
    }

    @Test
    void merge_all_sorts_results() {
        IntelliLangInjection injection1 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("ZClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();

        IntelliLangInjection injection2 = IntelliLangInjection.builder()
                .language("JSON")
                .displayName("AClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build();

        IntelliLangInjection injection3 = IntelliLangInjection.builder()
                .language("RegExp")
                .displayName("MClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern3"))
                .build();

        List<IntelliLangInjection> expected = List.of(injection2, injection3, injection1);
        List<IntelliLangInjection> actual = IntelliLangInjection.mergeAll(List.of(injection1, injection2, injection3));

        assertThat(actual).as("mergeAll should sort results by display name").isEqualTo(expected);
    }

    @Test
    void merge_all_with_different_injector_ids() {
        IntelliLangInjection injection1 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .injectorId("java")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();

        IntelliLangInjection injection2 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .injectorId("kotlin")
                .addPlaces(IntelliLangPlace.of("pattern2"))
                .build();

        List<IntelliLangInjection> expected = List.of(injection1, injection2);
        List<IntelliLangInjection> actual = IntelliLangInjection.mergeAll(List.of(injection1, injection2));

        assertThat(actual)
                .as("mergeAll should keep injections with different injector IDs separate")
                .isEqualTo(expected);
    }
}
