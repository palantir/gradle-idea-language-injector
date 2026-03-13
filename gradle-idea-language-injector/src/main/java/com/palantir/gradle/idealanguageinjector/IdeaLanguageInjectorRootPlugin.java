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

import com.palantir.gradle.idealanguageinjector.intellilang.UpdateIntelliLang;
import com.palantir.gradle.idealanguageinjector.scan.AnnotationScanTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

public abstract class IdeaLanguageInjectorRootPlugin implements Plugin<Project> {

    static final String CONVERTED_TO_XML = "idea-language-injector-jars";

    @Override
    public final void apply(Project rootProject) {
        if (rootProject != rootProject.getRootProject()) {
            throw new GradleException(
                    "The com.palantir.idea-language-injector plugin must be applied on the root project");
        }

        registerTransform(rootProject);

        NamedDomainObjectProvider<DependencyScopeConfiguration> subprojectDependencies =
                rootProject.getConfigurations().dependencyScope("idea-language-injector-subprojects");

        // to make this plugin isolated projects compatible instead of applying the project plugin here apply via a
        // settings plugin
        rootProject.allprojects(subproject -> {
            subproject.getPlugins().withType(JavaPlugin.class, _javaPlugin -> {
                subproject.getPlugins().apply(IdeaLanguageInjectorProjectPlugin.class);
            });
        });

        subprojectDependencies.configure(subprojectDeps -> {
            subprojectDeps
                    .getDependencies()
                    .addAllLater(rootProject.provider(() -> rootProject.getAllprojects().stream()
                            .map(subproject -> {
                                ProjectDependency dep = (ProjectDependency) rootProject
                                        .getDependencies()
                                        .project(Map.of(
                                                "path", subproject.getIsolated().getPath()));
                                dep.capabilities(caps -> {
                                    caps.requireCapability(IdeaLanguageInjectorProjectPlugin.OUTGOING_CAPABILITY_GROUP
                                            + ":"
                                            + IdeaLanguageInjectorProjectPlugin.OUTGOING_CAPABILITY_NAME);
                                });
                                return (Dependency) dep;
                            })
                            .toList()));
        });

        TaskProvider<UpdateIntelliLang> update = rootProject
                .getTasks()
                .register("updateIntelliLangXml", UpdateIntelliLang.class, task -> {
                    task.getArtifactFiles()
                            .from(rootProject
                                    .getConfigurations()
                                    .resolvable("collected-idea-language-injector-outgoing", conf -> {
                                        conf.extendsFrom(subprojectDependencies.get());
                                        conf.attributes(attrs -> {
                                            attrs.attribute(
                                                    Usage.USAGE_ATTRIBUTE,
                                                    rootProject.getObjects().named(Usage.class, Usage.JAVA_API));
                                            attrs.attribute(
                                                    ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                                                    ArtifactTypeDefinition.JAR_TYPE);
                                        });
                                    })
                                    .map(resolvable -> resolvable
                                            .getIncoming()
                                            .artifactView(view -> {
                                                view.lenient(true);
                                                view.attributes(attrs -> attrs.attribute(
                                                        ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                                                        CONVERTED_TO_XML));
                                            })
                                            .getFiles()));
                });

        if (Boolean.getBoolean("idea.active") && Boolean.getBoolean("idea.sync.active")) {
            // Add the tasks to the Gradle start parameters so they execute automatically.
            StartParameter startParameter = rootProject.getGradle().getStartParameter();
            List<String> taskNames = new ArrayList<>(startParameter.getTaskNames());
            taskNames.add(":" + update.getName());
            startParameter.setTaskNames(taskNames);
        }
    }

    private static void registerTransform(Project rootProject) {
        rootProject.getDependencies().registerTransform(AnnotationScanTransform.class, spec -> {
            spec.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE);
            spec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, CONVERTED_TO_XML);
        });
    }
}
