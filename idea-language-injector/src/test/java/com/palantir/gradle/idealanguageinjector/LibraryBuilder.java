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

import com.palantir.gradle.testing.execution.GradleInvoker;
import com.palantir.gradle.testing.project.RootProject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LibraryBuilder {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+);");

    private final Path projectDir;
    private final String group;
    private final String artifact;
    private final String version;
    private final Path mavenRepo;
    private final GradleInvoker gradleInvoker;
    private final List<String> resourceFiles = new ArrayList<>();

    LibraryBuilder(
            Path projectDir, String group, String artifact, String version, Path mavenRepo, GradleInvoker gradleInvoker) {
        this.projectDir = projectDir;
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.mavenRepo = mavenRepo;
        this.gradleInvoker = gradleInvoker;
    }

    LibraryBuilder withResource(String resourceFileName) {
        resourceFiles.add(resourceFileName);
        return this;
    }

    void build() {
        createBuildFile();
        copyResourceFiles();
        publish();
    }

    private void createBuildFile() {
        RootProject libProject = new RootProject(projectDir);

        libProject.settingsGradle().overwrite("rootProject.name = '" + artifact + "'");

        libProject
                .buildGradle()
                .overwrite(
                        """
                plugins {
                    id 'java-library'
                    id 'maven-publish'
                }

                group = '%s'
                version = '%s'

                repositories { mavenCentral() }
                dependencies { compileOnly 'org.jetbrains:annotations:24.0.1' }

                publishing {
                    publications {
                        maven(MavenPublication) {
                            from components.java
                            groupId = '%s'
                            artifactId = '%s'
                        }
                    }
                    repositories {
                        maven { url = uri('%s') }
                    }
                }
                """
                                .formatted(group, version, group, artifact, mavenRepo.toUri()));
    }

    private void copyResourceFiles() {
        for (String fileName : resourceFiles) {
            String content = readResource("/test-libraries/" + fileName);
            String packagePath = extractPackagePath(content);

            Path targetFile = projectDir.resolve("src/main/java").resolve(packagePath).resolve(fileName);

            try {
                Files.createDirectories(targetFile.getParent());
                Files.writeString(targetFile, content, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write file: " + targetFile, e);
            }
        }
    }

    private static String readResource(String resourcePath) {
        try (InputStream is = LibraryBuilder.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource: " + resourcePath, e);
        }
    }

    private static String extractPackagePath(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).replace('.', '/');
        }
        return "";
    }

    private void publish() {
        gradleInvoker.withArgs("publish", "--stacktrace").buildsSuccessfully();
    }
}
