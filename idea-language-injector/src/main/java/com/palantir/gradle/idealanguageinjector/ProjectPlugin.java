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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;

public class ProjectPlugin implements Plugin<Project> {

    private static final String SCANNED_JAR_TYPE = "language-scanned-jar";
    private static final String LANGUAGE_SCANS_CONFIGURATION = "languageScans";
    private static final String COLLECT_LANGUAGE_SCANS_TASK = "collectLanguageScans";

    @Override
    public void apply(Project project) {
        // Register the transform for this project
        project.getPlugins().withType(JavaPlugin.class, _javaPlugin -> {
            registerTransform(project);
            TaskProvider<Sync> collectTask = createCollectTask(project);
            createOutgoingConfiguration(project, collectTask);
        });
    }

    private static void registerTransform(Project project) {
        project.getDependencies().registerTransform(LanguageScanTransform.class, spec -> {
            spec.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE);
            spec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, SCANNED_JAR_TYPE);
        });
    }

    private static TaskProvider<Sync> createCollectTask(Project project) {
        return project.getTasks().register(COLLECT_LANGUAGE_SCANS_TASK, Sync.class, task -> {
            task.setDescription("Collects language scan files from dependencies");
            task.setGroup("build");

            task.from(project.getConfigurations()
                    .named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
                    .map(conf -> conf.getIncoming()
                            .artifactView(view -> view.attributes(attrs ->
                                    attrs.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, SCANNED_JAR_TYPE)))
                            .getFiles()));

            DirectoryProperty outputDir = project.getObjects().directoryProperty();
            outputDir.set(project.getLayout().getBuildDirectory().dir("language-scans"));
            task.into(outputDir);
        });
    }

    private static void createOutgoingConfiguration(Project project, TaskProvider<Sync> collectTask) {
        // Create an outgoing configuration that provides the scanned artifacts
        project.getConfigurations().register(LANGUAGE_SCANS_CONFIGURATION, outgoing -> {
            outgoing.setCanBeConsumed(true);
            outgoing.setCanBeResolved(false);
            outgoing.setDescription("Provides language scan artifacts for IntelliJ language injection");

            // Add attributes to identify this configuration
            outgoing.attributes(attrs -> {
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "language-scans"));
                attrs.attribute(
                        Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, Category.LIBRARY));
            });

            // The outgoing artifacts are the output of the collect task
            outgoing.getOutgoing().artifact(collectTask);
        });
    }
}
