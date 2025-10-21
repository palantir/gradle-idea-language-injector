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

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.execution.InvocationResult;
import com.palantir.gradle.testing.execution.TaskOutcome;
import com.palantir.gradle.testing.junit.GradlePluginTests;
import com.palantir.gradle.testing.project.RootProject;
import com.palantir.gradle.testing.project.SubProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("LineLength")
@GradlePluginTests
class IdeaLanguageInjectorTest {
    private static final String SIMPLE_LIB = "com.example:simple-lib:1.0.0";
    private static final String COMPLEX_LIB = "com.example:complex-lib:1.0.0";
    private static final String NO_ANNOTATIONS_LIB = "com.example:noannotations-lib:1.0.0";

    @BeforeEach
    void beforeEach(RootProject rootProject) {
        rootProject.buildGradle().append("""
            plugins {
                id 'java'
                id 'com.palantir.idea-language-injector'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation 'org.jetbrains:annotations:24.0.1'
            }
            """);
    }

    @Test
    void handles_dependencies_without_annotations_gracefully(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().appendLine("dependencies { implementation '" + NO_ANNOTATIONS_LIB + "' }");

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        rootProject
                .file(".idea/IntelliLang.xml")
                .assertThat()
                .as("IntelliLang.xml should not be created when there are no language annotations")
                .doesNotExist();
    }

    @Test
    void creates_IntelliLang_xml_with_proper_injection_patterns(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().appendLine("dependencies { implementation '" + COMPLEX_LIB + "' }");

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        // language=xml
        String expected = """
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="HTML" injector-id="java">
                  <display-name>ComplexLib (com.example.complex)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("executeStatic").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib"))]]></place>
                </injection>
                <injection language="JSON" injector-id="java">
                  <display-name>ComplexLib (com.example.complex)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("ComplexLib").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("process").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("process").withParameters("java.lang.String", "int").definedInClass("com.example.complex.ComplexLib"))]]></place>
                </injection>
                <injection language="SQL" injector-id="java">
                  <display-name>ComplexLib (com.example.complex)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("execute").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.complex.ComplexLib"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("queryVarargs").withParameters("java.lang.String", "java.lang.Object[]").definedInClass("com.example.complex.ComplexLib"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(1, psiMethod().withName("execute").withParameters("java.lang.String", "java.lang.String").definedInClass("com.example.complex.ComplexLib"))]]></place>
                </injection>
                <injection language="XML" injector-id="java">
                  <display-name>ComplexLib (com.example.complex)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("queryWithArray").withParameters("java.lang.String", "int[]").definedInClass("com.example.complex.ComplexLib"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("queryWithList").withParameters("java.lang.String", "java.util.List").definedInClass("com.example.complex.ComplexLib"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(1, psiMethod().withName("ComplexLib").withParameters("int", "java.lang.String").definedInClass("com.example.complex.ComplexLib"))]]></place>
                </injection>
                <injection language="HTML" injector-id="java">
                  <display-name>Inner (com.example.complex.ComplexLib)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("Inner").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib.Inner"))]]></place>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("render").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib.Inner"))]]></place>
                </injection>
                <injection language="JSON" injector-id="java">
                  <display-name>Nested (com.example.complex.ComplexLib)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("Nested").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib.Nested"))]]></place>
                </injection>
                <injection language="XML" injector-id="java">
                  <display-name>Nested (com.example.complex.ComplexLib)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("compile").withParameters("java.lang.String").definedInClass("com.example.complex.ComplexLib.Nested"))]]></place>
                </injection>
              </component>
            </project>
            """.trim();

        String actual = rootProject.file(".idea/IntelliLang.xml").text().trim();
        assertThat(actual)
                .as("Generated IntelliLang.xml should match expected injection patterns")
                .isEqualTo(expected);
    }

    @Test
    void scans_subproject_dependencies(GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        subProject.buildGradle().append("""
            plugins {
                id 'java'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation '%s'
            }
            """.formatted(SIMPLE_LIB));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual)
                .as("IntelliLang.xml should contain SimpleLib display name")
                .contains("SimpleLib (com.example.simple)");
    }

    @Test
    void transform_is_cacheable(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().appendLine("dependencies { implementation '" + SIMPLE_LIB + "' }");

        InvocationResult firstRun =
                gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();
        InvocationResult secondRun =
                gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        assertThat(firstRun.task(":updateIntelliLangXml"))
                .as("First run should execute the task")
                .hasValueSatisfying(task -> assertThat(task.outcome())
                        .as("First run task outcome should not be cached")
                        .isNotIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE));
        assertThat(secondRun.task(":updateIntelliLangXml"))
                .as("Second run should use cached result")
                .hasValueSatisfying(task -> assertThat(task.outcome())
                        .as("Second run task outcome should be cached")
                        .isIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE));
    }

    @Test
    void merges_multiple_libraries(GradleInvoker gradle, RootProject rootProject) {
        rootProject.buildGradle().append("""
            dependencies {
                implementation '%s'
                implementation '%s'
            }
            """.formatted(SIMPLE_LIB, COMPLEX_LIB));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual)
                .as("IntelliLang.xml should contain SimpleLib display name")
                .contains("SimpleLib (com.example.simple)");
        assertThat(actual)
                .as("IntelliLang.xml should contain ComplexLib display name")
                .contains("ComplexLib (com.example.complex)");
    }

    @Test
    void handles_subproject_without_dependencies(GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        subProject.buildGradle().append("""
            plugins {
                id 'java'
            }
            """);

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        rootProject
                .file(".idea/IntelliLang.xml")
                .assertThat()
                .as("IntelliLang.xml should not be created when subproject has no dependencies")
                .doesNotExist();
    }

    @Test
    void handles_multiple_subprojects_with_mixed_dependencies(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject1, SubProject subProject2) {
        subProject1.buildGradle().append("""
            plugins {
                id 'java'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation '%s'
            }
            """.formatted(SIMPLE_LIB));

        subProject2.buildGradle().append("""
            plugins {
                id 'java'
            }
            """);

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual)
                .as("IntelliLang.xml should contain SimpleLib from subproject with dependencies")
                .contains("SimpleLib (com.example.simple)");
    }

    @Test
    void aggregates_dependencies_from_root_and_subproject(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        rootProject.buildGradle().appendLine("dependencies { implementation '" + SIMPLE_LIB + "' }");
        subProject.buildGradle().append("""
            plugins {
                id 'java'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation '%s'
            }
            """.formatted(COMPLEX_LIB));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual)
                .as("IntelliLang.xml should contain SimpleLib from root project")
                .contains("SimpleLib (com.example.simple)");
        assertThat(actual)
                .as("IntelliLang.xml should contain ComplexLib from subproject")
                .contains("ComplexLib (com.example.complex)");
    }

    @Test
    void handles_subproject_with_non_annotation_dependencies(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        subProject.buildGradle().append("""
            plugins {
                id 'java'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation '%s'
            }
            """.formatted(NO_ANNOTATIONS_LIB));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        rootProject
                .file(".idea/IntelliLang.xml")
                .assertThat()
                .as("IntelliLang.xml should not be created when subproject dependencies have no annotations")
                .doesNotExist();
    }

    @Test
    void deduplicates_same_library_from_multiple_subprojects(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject1, SubProject subProject2) {
        subProject1.buildGradle().append("""
            plugins {
                id 'java'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation '%s'
            }
            """.formatted(SIMPLE_LIB));

        subProject2.buildGradle().append("""
            plugins {
                id 'java'
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            dependencies {
                implementation '%s'
            }
            """.formatted(SIMPLE_LIB));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();

        // language=xml
        String expected = """
            <project version="4">
              <component name="LanguageInjectionConfiguration">
                <injection language="HTML" injector-id="java">
                  <display-name>SimpleLib (com.example.simple)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("renderHtml").withParameters("java.lang.String").definedInClass("com.example.simple.SimpleLib"))]]></place>
                </injection>
                <injection language="SQL" injector-id="java">
                  <display-name>SimpleLib (com.example.simple)</display-name>
                  <single-file value="false"/>
                  <place><![CDATA[psiParameter().ofMethod(0, psiMethod().withName("query").withParameters("java.lang.String").definedInClass("com.example.simple.SimpleLib"))]]></place>
                </injection>
              </component>
            </project>
            """;

        assertThat(expected).as("IntelliLang.xml should match expected").isEqualTo(actual);
    }
}
