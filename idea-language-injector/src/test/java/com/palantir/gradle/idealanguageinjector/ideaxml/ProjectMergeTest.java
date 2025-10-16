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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ProjectMergeTest {

    @Test
    void project_merge_all_combines_all_injections() {
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

        Injection injection3 = Injection.builder()
                .language("JSON")
                .displayName("OtherClass (com.example)")
                .addPlaces(Place.of("pattern3"))
                .build();

        Project project1 = Project.of(Component.of(List.of(injection1)));
        Project project2 = Project.of(Component.of(List.of(injection2)));
        Project project3 = Project.of(Component.of(List.of(injection3)));

        Project merged = Project.mergeAll(List.of(project1, project2, project3));

        assertThat(merged.component().injections()).hasSize(2);
        // Find the SQL injection (should have merged places)
        Injection sqlInjection = merged.component().injections().stream()
                .filter(i -> i.language().equals("SQL"))
                .findFirst()
                .orElseThrow();
        assertThat(sqlInjection.places()).hasSize(2);
    }

    @Test
    void project_merge_all_uses_default_version() {
        Project project1 = Project.of(Component.of(List.of()));
        Project project2 = Project.of(Component.of(List.of()));

        Project merged = Project.mergeAll(List.of(project1, project2));

        assertThat(merged.version()).isEqualTo("4");
    }

    @Test
    void project_merge_all_with_empty_list() {
        Project merged = Project.mergeAll(List.of());

        assertThat(merged.component().injections()).isEmpty();
        assertThat(merged.version()).isEqualTo("4");
    }

    @Test
    void project_merge_all_handles_empty_projects() {
        Project emptyProject1 = Project.empty();
        Project emptyProject2 = Project.empty();

        Injection injection = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern1"))
                .build();
        Project projectWithData = Project.of(Component.of(List.of(injection)));

        Project merged = Project.mergeAll(List.of(emptyProject1, projectWithData, emptyProject2));

        assertThat(merged.component().injections()).hasSize(1);
        assertThat(merged.component().injections().get(0).language()).isEqualTo("SQL");
    }

    @Test
    void project_empty() {
        Project empty = Project.empty();

        assertThat(empty.component().injections()).isEmpty();
        assertThat(empty.version()).isEqualTo("4");
    }

    @Test
    void project_of_with_component() {
        Injection injection = Injection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(Place.of("pattern1"))
                .build();

        Component component = Component.of(List.of(injection));
        Project project = Project.of(component);

        assertThat(project.component()).isEqualTo(component);
        assertThat(project.version()).isEqualTo("4");
    }

    @Test
    void project_merge_all_integration_with_duplicates() {
        Injection injection1a = Injection.builder()
                .language("SQL")
                .displayName("QueryBuilder (com.db)")
                .addPlaces(Place.of("psiParameter().ofMethod(0, psiMethod().withName(\"query\"))"))
                .build();

        Injection injection1b = Injection.builder()
                .language("SQL")
                .displayName("QueryBuilder (com.db)")
                .addPlaces(Place.of("psiParameter().ofMethod(0, psiMethod().withName(\"query\"))"))
                .build();

        Injection injection2 = Injection.builder()
                .language("SQL")
                .displayName("QueryBuilder (com.db)")
                .addPlaces(Place.of("psiParameter().ofMethod(0, psiMethod().withName(\"execute\"))"))
                .build();

        Project project1 = Project.of(Component.of(List.of(injection1a)));
        Project project2 = Project.of(Component.of(List.of(injection1b, injection2)));

        Project merged = Project.mergeAll(List.of(project1, project2));

        assertThat(merged.component().injections()).hasSize(1);
        assertThat(merged.component().injections().get(0).places()).hasSize(2);
        // Verify duplicate pattern was removed
        assertThat(merged.component().injections().get(0).places().stream().map(Place::pattern))
                .containsExactlyInAnyOrder(
                        "psiParameter().ofMethod(0, psiMethod().withName(\"query\"))",
                        "psiParameter().ofMethod(0, psiMethod().withName(\"execute\"))");
    }
}
