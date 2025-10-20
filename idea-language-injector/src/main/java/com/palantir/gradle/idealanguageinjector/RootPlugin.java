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
import java.util.ArrayList;
import java.util.List;
import org.gradle.StartParameter;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public final class RootPlugin implements Plugin<Project> {
    @Override
    public void apply(Project rootProject) {
        NamedDomainObjectProvider<DependencyScopeConfiguration> subprojectDependencies =
                rootProject.getConfigurations().dependencyScope("intellilang-subproject");

        rootProject.allprojects(subproject -> {
            rootProject.getDependencies().add(subprojectDependencies.getName(), subproject);
        });

        Provider<FileCollection> files = rootProject
                .getConfigurations()
                .resolvable("intellilangResolvable", conf -> {
                    conf.extendsFrom(subprojectDependencies.get());
                    conf.attributes(attrs -> {
                        attrs.attribute(
                                Usage.USAGE_ATTRIBUTE,
                                rootProject.getObjects().named(Usage.class, ProjectPlugin.LANGUAGE_ANNOTATION_SCANS));
                    });
                })
                .map(resolvable -> resolvable
                        .getIncoming()
                        .artifactView(view -> {
                            view.lenient(true);
                        })
                        .getFiles());

        TaskProvider<UpdateIntelliLang> update = rootProject
                .getTasks()
                .register("updateIntelliLangXml", UpdateIntelliLang.class, task -> {
                    task.getArtifactFiles().from(files);
                });

        if (!Boolean.getBoolean("idea.active") || !Boolean.getBoolean("idea.sync.active")) {
            // Add the tasks to the Gradle start parameters so they execute automatically.
            StartParameter startParameter = rootProject.getGradle().getStartParameter();
            List<String> taskNames = new ArrayList<>(startParameter.getTaskNames());
            taskNames.add(":" + update.getName());
            startParameter.setTaskNames(taskNames);
        }
    }
}
