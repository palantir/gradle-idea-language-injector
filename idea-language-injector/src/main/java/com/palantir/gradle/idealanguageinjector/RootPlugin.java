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

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;

public class RootPlugin implements Plugin<Project> {
    @Override
    public void apply(Project rootProject) {
        NamedDomainObjectProvider<DependencyScopeConfiguration> subprojectDependencies =
                rootProject.getConfigurations().dependencyScope("intellilang-subproject");

        rootProject.allprojects(subproject -> {
            rootProject.getDependencies().add(subprojectDependencies.getName(), subproject);
        });

        NamedDomainObjectProvider<ResolvableConfiguration> resolvable = rootProject
                .getConfigurations()
                .resolvable("intellilangResolvable", conf -> {
                    conf.extendsFrom(subprojectDependencies.get());
                    conf.attributes(attrs -> {
                        attrs.attribute(
                                Usage.USAGE_ATTRIBUTE,
                                rootProject.getObjects().named(Usage.class, ProjectPlugin.LANGUAGE_SCANS));
                        attrs.attribute(
                                Category.CATEGORY_ATTRIBUTE,
                                rootProject.getObjects().named(Category.class, Category.LIBRARY));
                    });
                });

        rootProject.getTasks().register("updateIntelliLangXml", UpdateIntelliLangXml.class, task -> {
            task.getArtifactFiles().from(resolvable);
        });
    }
}
