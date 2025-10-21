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
import org.gradle.StartParameter;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

public final class IdeaLanguageInjectorRootPlugin implements Plugin<Project> {

    static final String CONVERTED_TO_XML = "idea-language-injector-jars";

    @Override
    public void apply(Project rootProject) {
        registerTransform(rootProject);

        NamedDomainObjectProvider<DependencyScopeConfiguration> subprojectDependencies =
                rootProject.getConfigurations().dependencyScope("idea-language-injector-subprojects");

        rootProject.allprojects(subproject -> {
            subproject.getPlugins().withType(JavaPlugin.class, _javaPlugin -> {
                subproject.getPlugins().apply(IdeaLanguageInjectorProjectPlugin.class);
                rootProject.getDependencies().add(subprojectDependencies.getName(), subproject);
            });
        });

        TaskProvider<UpdateIntelliLang> update = rootProject
                .getTasks()
                .register("updateIntelliLangXml", UpdateIntelliLang.class, task -> {
                    task.getArtifactFiles()
                            .from(rootProject
                                    .getConfigurations()
                                    .resolvable("collected-idea-language-injector-outgoing", conf -> {
                                        conf.extendsFrom(subprojectDependencies.get());
                                        conf.setTransitive(false);
                                        conf.attributes(attrs -> {
                                            attrs.attribute(
                                                    Usage.USAGE_ATTRIBUTE,
                                                    rootProject
                                                            .getObjects()
                                                            .named(
                                                                    Usage.class,
                                                                    IdeaLanguageInjectorProjectPlugin.OUTGOING_USAGE));
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
