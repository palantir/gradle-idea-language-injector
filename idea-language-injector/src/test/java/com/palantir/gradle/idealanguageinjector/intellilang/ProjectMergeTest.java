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

public class ProjectMergeTest {

    @Test
    void project_merge_all_combines_all_injections() {
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

        IntelliLangInjection injection3 = IntelliLangInjection.builder()
                .language("JSON")
                .displayName("OtherClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern3"))
                .build();

        IntelliLangProject project1 = IntelliLangProject.of(IntelliLangComponent.of(List.of(injection1)));
        IntelliLangProject project2 = IntelliLangProject.of(IntelliLangComponent.of(List.of(injection2)));
        IntelliLangProject project3 = IntelliLangProject.of(IntelliLangComponent.of(List.of(injection3)));

        IntelliLangProject merged = IntelliLangProject.mergeAll(List.of(project1, project2, project3));

        assertThat(merged.component().injections()).hasSize(2);
        // Find the SQL injection (should have merged places)
        IntelliLangInjection sqlInjection = merged.component().injections().stream()
                .filter(i -> i.language().equals("SQL"))
                .findFirst()
                .orElseThrow();
        assertThat(sqlInjection.places()).hasSize(2);
    }

    @Test
    void project_merge_all_uses_default_version() {
        IntelliLangProject project1 = IntelliLangProject.of(IntelliLangComponent.of(List.of()));
        IntelliLangProject project2 = IntelliLangProject.of(IntelliLangComponent.of(List.of()));

        IntelliLangProject merged = IntelliLangProject.mergeAll(List.of(project1, project2));

        assertThat(merged.version()).isEqualTo("4");
    }

    @Test
    void project_merge_all_with_empty_list() {
        IntelliLangProject merged = IntelliLangProject.mergeAll(List.of());

        assertThat(merged.component().injections()).isEmpty();
        assertThat(merged.version()).isEqualTo("4");
    }

    @Test
    void project_merge_all_handles_empty_projects() {
        IntelliLangProject emptyProject1 = IntelliLangProject.empty();
        IntelliLangProject emptyProject2 = IntelliLangProject.empty();

        IntelliLangInjection injection = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();
        IntelliLangProject projectWithData = IntelliLangProject.of(IntelliLangComponent.of(List.of(injection)));

        IntelliLangProject merged = IntelliLangProject.mergeAll(List.of(emptyProject1, projectWithData, emptyProject2));

        assertThat(merged.component().injections()).hasSize(1);
        assertThat(merged.component().injections().get(0).language()).isEqualTo("SQL");
    }

    @Test
    void project_empty() {
        IntelliLangProject empty = IntelliLangProject.empty();

        assertThat(empty.component().injections()).isEmpty();
        assertThat(empty.version()).isEqualTo("4");
    }

    @Test
    void project_of_with_component() {
        IntelliLangInjection injection = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();

        IntelliLangComponent component = IntelliLangComponent.of(List.of(injection));
        IntelliLangProject project = IntelliLangProject.of(component);

        assertThat(project.component()).isEqualTo(component);
        assertThat(project.version()).isEqualTo("4");
    }

    @Test
    void project_merge_all_integration_with_duplicates() {
        IntelliLangInjection injection1a = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("QueryBuilder (com.db)")
                .addPlaces(IntelliLangPlace.of("psiParameter().ofMethod(0, psiMethod().withName(\"query\"))"))
                .build();

        IntelliLangInjection injection1b = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("QueryBuilder (com.db)")
                .addPlaces(IntelliLangPlace.of("psiParameter().ofMethod(0, psiMethod().withName(\"query\"))"))
                .build();

        IntelliLangInjection injection2 = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("QueryBuilder (com.db)")
                .addPlaces(IntelliLangPlace.of("psiParameter().ofMethod(0, psiMethod().withName(\"execute\"))"))
                .build();

        IntelliLangProject project1 = IntelliLangProject.of(IntelliLangComponent.of(List.of(injection1a)));
        IntelliLangProject project2 = IntelliLangProject.of(IntelliLangComponent.of(List.of(injection1b, injection2)));

        IntelliLangProject merged = IntelliLangProject.mergeAll(List.of(project1, project2));

        assertThat(merged.component().injections()).hasSize(1);
        assertThat(merged.component().injections().get(0).places()).hasSize(2);
        // Verify duplicate pattern was removed
        assertThat(merged.component().injections().get(0).places().stream().map(IntelliLangPlace::pattern))
                .containsExactlyInAnyOrder(
                        "psiParameter().ofMethod(0, psiMethod().withName(\"query\"))",
                        "psiParameter().ofMethod(0, psiMethod().withName(\"execute\"))");
    }
}
