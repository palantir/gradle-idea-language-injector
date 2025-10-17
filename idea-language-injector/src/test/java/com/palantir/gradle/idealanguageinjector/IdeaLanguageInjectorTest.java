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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("LineLength")
@GradlePluginTests
class IdeaLanguageInjectorTest {
    private Path localRepo;

    @BeforeEach
    void beforeEach(RootProject rootProject) throws IOException {
        localRepo = Files.createTempDirectory("maven-repo");

        rootProject.gradlePropertiesFile().appendLine("org.gradle.unsafe.isolated-projects=true");

        rootProject.settingsGradle().edit(content -> """
            plugins {
                id 'com.palantir.idea-language-injector'
            }
            """ + content);

        // Setup build file
        rootProject.buildGradle().append("""
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
            """.formatted(localRepo.toUri()));
    }

    @Test
    void handles_dependencies_without_annotations_gracefully(GradleInvoker gradle, RootProject rootProject)
            throws IOException {
        String library = publishLibrary("no-annotations", "NoAnnotations.java");
        rootProject.buildGradle().appendLine("dependencies { implementation '" + library + "' }");

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        assertThat(rootProject.file(".idea/IntelliLang.xml").path()).doesNotExist();
    }

    @Test
    void creates_IntelliLang_xml_with_proper_injection_patterns(GradleInvoker gradle, RootProject rootProject)
            throws IOException {
        String library = publishLibrary("complex-lib", "ComplexLib.java");
        rootProject.buildGradle().appendLine("dependencies { implementation '" + library + "' }");

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
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void scans_subproject_dependencies(GradleInvoker gradle, RootProject rootProject, SubProject subProject)
            throws IOException {
        String library = publishLibrary("simple-lib", "SimpleLib.java");

        subProject.buildGradle().append("""
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
            """.formatted(localRepo.toUri(), library));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual).contains("SimpleLib (com.example.simple)");
        assertThat(actual).contains(".withName(\"query\")");
        assertThat(actual).contains(".withName(\"renderHtml\")");
    }

    @Test
    void transform_is_cacheable(GradleInvoker gradle, RootProject rootProject) throws IOException {
        String library = publishLibrary("simple-lib", "SimpleLib.java");
        rootProject.buildGradle().appendLine("dependencies { implementation '" + library + "' }");

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        InvocationResult result =
                gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        assertThat(result.task(":updateIntelliLangXml")).hasValueSatisfying(task -> assertThat(task.outcome())
                .isIn(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE));
    }

    @Test
    void merges_multiple_libraries(GradleInvoker gradle, RootProject rootProject) throws IOException {
        String simpleLib = publishLibrary("simple-lib", "SimpleLib.java");
        String complexLib = publishLibrary("complex-lib", "ComplexLib.java");

        rootProject.buildGradle().append("""
            dependencies {
                implementation '%s'
                implementation '%s'
            }
            """.formatted(simpleLib, complexLib));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual).contains("SimpleLib (com.example.simple)");
        assertThat(actual).contains("ComplexLib (com.example.complex)");
    }

    @Test
    void handles_subproject_without_dependencies(GradleInvoker gradle, RootProject rootProject, SubProject subProject) {
        subProject.buildGradle().append("""
            plugins {
                id 'java'
            }
            """);

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        assertThat(rootProject.file(".idea/IntelliLang.xml").path()).doesNotExist();
    }

    @Test
    void handles_multiple_subprojects_with_mixed_dependencies(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject1, SubProject subProject2)
            throws IOException {
        String library = publishLibrary("simple-lib", "SimpleLib.java");

        subProject1.buildGradle().append("""
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
            """.formatted(localRepo.toUri(), library));

        subProject2.buildGradle().append("""
            plugins {
                id 'java'
            }
            """);

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual).contains("SimpleLib (com.example.simple)");
    }

    @Test
    void aggregates_dependencies_from_root_and_subproject(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject) throws IOException {
        String simpleLib = publishLibrary("simple-lib", "SimpleLib.java");
        String complexLib = publishLibrary("complex-lib", "ComplexLib.java");

        rootProject.buildGradle().appendLine("dependencies { implementation '" + simpleLib + "' }");
        subProject.buildGradle().append("""
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
            """.formatted(localRepo.toUri(), complexLib));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        String actual = rootProject.file(".idea/IntelliLang.xml").text();
        assertThat(actual).contains("SimpleLib (com.example.simple)");
        assertThat(actual).contains("ComplexLib (com.example.complex)");
    }

    @Test
    void handles_subproject_with_non_annotation_dependencies(
            GradleInvoker gradle, RootProject rootProject, SubProject subProject) throws IOException {
        String library = publishLibrary("no-annotations", "NoAnnotations.java");

        subProject.buildGradle().append("""
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
            """.formatted(localRepo.toUri(), library));

        gradle.withArgs("-Didea.active=true", "-Didea.sync.active=true").buildsSuccessfully();

        assertThat(rootProject.file(".idea/IntelliLang.xml").path()).doesNotExist();
    }

    private String publishLibrary(String artifact, String... resourceFiles) throws IOException {
        Path tempDir = Files.createTempDirectory("lib-" + artifact);
        Path srcDir = tempDir.resolve("src");
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);

        // Compile sources
        List<File> sources = new ArrayList<>();
        for (String fileName : resourceFiles) {
            String content = readResource("/test-libraries/" + fileName);
            Matcher pkgMatcher = Pattern.compile("package\\s+([\\w.]+);").matcher(content);
            String pkgPath = pkgMatcher.find() ? pkgMatcher.group(1).replace('.', '/') : "";

            Path srcFile = srcDir.resolve(pkgPath).resolve(fileName);
            Files.createDirectories(srcFile.getParent());
            Files.writeString(srcFile, content);
            sources.add(srcFile.toFile());
        }

        // Compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(binDir.toFile()));
            compiler.getTask(
                            null,
                            fm,
                            null,
                            List.of("-cp", System.getProperty("java.class.path")),
                            null,
                            fm.getJavaFileObjectsFromFiles(sources))
                    .call();
        }

        // Create JAR and POM
        Path repoDir = localRepo.resolve("com/example/" + artifact + "/1.0.0");
        Files.createDirectories(repoDir);

        try (JarOutputStream jar =
                        new JarOutputStream(Files.newOutputStream(repoDir.resolve(artifact + "-1.0.0.jar")));
                Stream<Path> stream = Files.walk(binDir)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                jar.putNextEntry(new JarEntry(binDir.relativize(file).toString().replace(File.separatorChar, '/')));
                Files.copy(file, jar);
                jar.closeEntry();
            }
        }

        Files.writeString(
                repoDir.resolve(artifact + "-1.0.0.pom"),
                // language=xml
                """
                <?xml version="1.0"?>
                <project><modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId><artifactId>%s</artifactId><version>1.0.0</version>
                </project>
                """.formatted(artifact));

        return "com.example:" + artifact + ":1.0.0";
    }

    private static String readResource(String path) throws IOException {
        try (InputStream is = IdeaLanguageInjectorTest.class.getResourceAsStream(path)) {
            Assertions.assertNotNull(is, "Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
