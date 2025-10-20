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

        IntelliLangProject expected = IntelliLangProject.of(IntelliLangComponent.of(List.of(
                IntelliLangInjection.builder()
                        .language("JSON")
                        .displayName("OtherClass (com.example)")
                        .addPlaces(IntelliLangPlace.of("pattern3"))
                        .build(),
                IntelliLangInjection.builder()
                        .language("SQL")
                        .displayName("TestClass (com.example)")
                        .addPlaces(IntelliLangPlace.of("pattern1"))
                        .addPlaces(IntelliLangPlace.of("pattern2"))
                        .build())));
        IntelliLangProject actual = IntelliLangProject.mergeAll(List.of(project1, project2, project3));

        assertThat(actual)
                .as("mergeAll should combine injections with same key and keep separate ones")
                .isEqualTo(expected);
    }

    @Test
    void project_merge_all_uses_default_version() {
        IntelliLangProject project1 = IntelliLangProject.of(IntelliLangComponent.of(List.of()));
        IntelliLangProject project2 = IntelliLangProject.of(IntelliLangComponent.of(List.of()));

        String expected = "4";
        String actual = IntelliLangProject.mergeAll(List.of(project1, project2)).version();

        assertThat(actual).as("merged project should have default version").isEqualTo(expected);
    }

    @Test
    void project_merge_all_with_empty_list() {
        IntelliLangProject expected = IntelliLangProject.empty();
        IntelliLangProject actual = IntelliLangProject.mergeAll(List.of());

        assertThat(actual)
                .as("mergeAll with empty list should return empty project")
                .isEqualTo(expected);
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

        IntelliLangProject actual = IntelliLangProject.mergeAll(List.of(emptyProject1, projectWithData, emptyProject2));

        assertThat(actual).as("mergeAll should handle empty projects correctly").isEqualTo(projectWithData);
    }

    @Test
    void project_empty() {
        IntelliLangProject expected = IntelliLangProject.of(IntelliLangComponent.of(List.of()));
        IntelliLangProject actual = IntelliLangProject.empty();

        assertThat(actual).as("empty project should have no injections").isEqualTo(expected);
    }

    @Test
    void project_of_with_component() {
        IntelliLangInjection injection = IntelliLangInjection.builder()
                .language("SQL")
                .displayName("TestClass (com.example)")
                .addPlaces(IntelliLangPlace.of("pattern1"))
                .build();

        IntelliLangComponent component = IntelliLangComponent.of(List.of(injection));

        IntelliLangComponent actualComponent = IntelliLangProject.of(component).component();
        assertThat(actualComponent)
                .as("project should contain the provided component")
                .isEqualTo(component);

        String expectedVersion = "4";
        String actualVersion = IntelliLangProject.of(component).version();
        assertThat(actualVersion).as("project should have default version").isEqualTo(expectedVersion);
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

        IntelliLangProject expected =
                IntelliLangProject.of(IntelliLangComponent.of(List.of(IntelliLangInjection.builder()
                        .language("SQL")
                        .displayName("QueryBuilder (com.db)")
                        .addPlaces(IntelliLangPlace.of("psiParameter().ofMethod(0, psiMethod().withName(\"execute\"))"))
                        .addPlaces(IntelliLangPlace.of("psiParameter().ofMethod(0, psiMethod().withName(\"query\"))"))
                        .build())));
        IntelliLangProject actual = IntelliLangProject.mergeAll(List.of(project1, project2));

        assertThat(actual)
                .as("mergeAll should deduplicate patterns when merging injections")
                .isEqualTo(expected);
    }
}
