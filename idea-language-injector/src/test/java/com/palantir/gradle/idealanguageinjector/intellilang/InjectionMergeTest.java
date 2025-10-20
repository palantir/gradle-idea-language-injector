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
        Injection injection1 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern1"))
                .build();

        Injection injection2 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern2"))
                .build();

        Injection merged = injection1.mergeWith(injection2);

        assertThat(merged.places()).hasSize(2);
        assertThat(merged.places().stream().map(Place::pattern)).containsExactlyInAnyOrder("pattern1", "pattern2");
    }

    @Test
    void merge_with_removes_duplicate_places() {
        Injection injection1 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern1"))
                .addPlaces(Place.of("pattern2"))
                .build();

        Injection injection2 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern2"))
                .addPlaces(Place.of("pattern3"))
                .build();

        Injection merged = injection1.mergeWith(injection2);

        assertThat(merged.places()).hasSize(3);
        assertThat(merged.places().stream().map(Place::pattern))
                .containsExactlyInAnyOrder("pattern1", "pattern2", "pattern3");
    }

    @Test
    void merge_all_combines_injections_with_same_key() {
        Injection injection1 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern1"))
                .build();

        Injection injection2 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern2"))
                .build();

        List<Injection> merged = Injection.mergeAll(List.of(injection1, injection2));

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).places()).hasSize(2);
        assertThat(merged.get(0).places().stream().map(Place::pattern))
                .containsExactlyInAnyOrder("pattern1", "pattern2");
    }

    @Test
    void merge_all_keeps_separate_injections_with_different_keys() {
        Injection injection1 = Injection.builder()
                .language("SQL")
                .displayName("TestClass1 (com.example)")
                .addPlaces(Place.of("pattern1"))
                .build();

        Injection injection2 = Injection.builder()
                .language("JSON")
                .displayName("TestClass2 (com.example)")
                .addPlaces(Place.of("pattern2"))
                .build();

        List<Injection> merged = Injection.mergeAll(List.of(injection1, injection2));

        assertThat(merged).hasSize(2);
    }

    @Test
    void merge_all_sorts_results() {
        Injection injection1 = Injection.builder()
                .language("SQL")
                .displayName("ZClass (com.example)")
                .addPlaces(Place.of("pattern1"))
                .build();

        Injection injection2 = Injection.builder()
                .language("JSON")
                .displayName("AClass (com.example)")
                .addPlaces(Place.of("pattern2"))
                .build();

        Injection injection3 = Injection.builder()
                .language("RegExp")
                .displayName("MClass (com.example)")
                .addPlaces(Place.of("pattern3"))
                .build();

        List<Injection> merged = Injection.mergeAll(List.of(injection1, injection2, injection3));

        assertThat(merged).hasSize(3);
        assertThat(merged.get(0).displayName()).isEqualTo("AClass (com.example)");
        assertThat(merged.get(1).displayName()).isEqualTo("MClass (com.example)");
        assertThat(merged.get(2).displayName()).isEqualTo("ZClass (com.example)");
    }

    @Test
    void merge_all_with_different_injector_ids() {
        Injection injection1 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .injectorId("java")
                .addPlaces(Place.of("pattern1"))
                .build();

        Injection injection2 = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .injectorId("kotlin")
                .addPlaces(Place.of("pattern2"))
                .build();

        List<Injection> merged = Injection.mergeAll(List.of(injection1, injection2));

        // Different injector IDs means different keys, so they should remain separate
        assertThat(merged).hasSize(2);
    }
}
