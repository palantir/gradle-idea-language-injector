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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@GradlePluginTests
class IdeaLanguageInjectorTest {
    private Path localRepo;

    @BeforeEach
    void beforeEach(RootProject rootProject) {
        try {
            localRepo = Files.createTempDirectory("maven-repo");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        rootProject
                .settingsGradle()
                .edit(content ->
                        """
                        plugins {
                            id 'com.palantir.idea-language-injector'
                        }
                        """
                                + content);

        // Setup build file
        rootProject
                .buildGradle()
                .overwrite(
                        """
                        plugins {
                            id 'java'
                        }

                        repositories {
                            maven { url = uri('%s') }
                            mavenCentral()
                        }

                        dependencies {
                            implementation 'org.jetbrains:annotations:24.0.1'
                        }
                        """
                                .formatted(localRepo.toUri()));
    }

    @Test
    void handles_dependencies_without_annotations_gracefully(GradleInvoker gradle, RootProject rootProject) {
        String library = publishLibrary("com.example", "no-annotations", "NoAnnotations.java");
        rootProject.buildGradle().appendLine("dependencies { implementation '" + library + "' }");

        gradle.withArgs("updateIntelliLangXml", "-Didea.active=true", "-Didea.sync.active=true")
                .buildsSuccessfully();

        assertThat(rootProject.file(".idea/IntelliLang.xml").path()).doesNotExist();
    }

    @Test
    void creates_IntelliLang_xml_with_proper_injection_patterns(GradleInvoker gradle, RootProject rootProject) {
        String library = publishLibrary("com.example", "complex-lib", "ComplexLib.java");
        rootProject.buildGradle().appendLine("dependencies { implementation '" + library + "' }");

        gradle.withArgs("updateIntelliLangXml", "-Didea.active=true", "-Didea.sync.active=true")
                .buildsSuccessfully();

        // language=xml
        String expected =
                """
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
                """
                        .trim();

        String actual = rootProject.file(".idea/IntelliLang.xml").text().trim();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void scans_subproject_dependencies(GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        String library = publishLibrary("com.example", "simple-lib", "SimpleLib.java");

        subProject
                .buildGradle()
                .overwrite(
                        """
                        plugins {
                            id 'java'
                        }

                        repositories {
                            maven { url = uri('%s') }
                            mavenCentral()
                        }

                        dependencies {
                            implementation '%s'
                        }
                        """
                                .formatted(localRepo.toUri(), library));

        gradle.withArgs("updateIntelliLangXml", "-Didea.active=true", "-Didea.sync.active=true")
                .buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual).contains("SimpleLib (com.example.simple)");
        assertThat(actual).contains(".withName(\"query\")");
        assertThat(actual).contains(".withName(\"renderHtml\")");
    }

    @Test
    void transform_is_cacheable(GradleInvoker gradle, RootProject rootProject) {
        String library = publishLibrary("com.example", "simple-lib", "SimpleLib.java");
        rootProject.buildGradle().appendLine("dependencies { implementation '" + library + "' }");

        gradle.withArgs("updateIntelliLangXml", "-Didea.active=true", "-Didea.sync.active=true")
                .buildsSuccessfully();

        InvocationResult result = gradle.withArgs(
                        "updateIntelliLangXml", "-Didea.active=true", "-Didea.sync.active=true")
                .buildsSuccessfully();

        assertThat(result.task(":updateIntelliLangXml")).hasValueSatisfying(task -> assertThat(task.outcome())
                .isIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE));
    }

    @Test
    void merges_multiple_libraries(GradleInvoker gradle, RootProject rootProject) {
        String simpleLib = publishLibrary("com.example", "simple-lib", "SimpleLib.java");
        String complexLib = publishLibrary("com.example", "complex-lib", "ComplexLib.java");

        rootProject
                .buildGradle()
                .append(
                        """
                        dependencies {
                            implementation '%s'
                            implementation '%s'
                        }
                        """
                                .formatted(simpleLib, complexLib));

        gradle.withArgs("updateIntelliLangXml", "-Didea.active=true", "-Didea.sync.active=true")
                .buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual).contains("SimpleLib (com.example.simple)");
        assertThat(actual).contains("ComplexLib (com.example.complex)");
    }

    private String publishLibrary(String group, String artifact, String... resourceFiles) {
        try {
            LibraryBuilder builder = new LibraryBuilder(group, artifact, localRepo);
            Arrays.stream(resourceFiles).forEach(builder::withResource);
            builder.build();

            return group + ":" + artifact + ":1.0.0";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
