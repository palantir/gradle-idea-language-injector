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

import java.io.File;
import java.util.Set;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;

public final class IdeaLanguageInjectorProjectPlugin implements Plugin<Project> {

    static final String OUTGOING_USAGE = "idea-language-injector-jars";
    static final Attribute<Boolean> HAS_LANGUAGE_ANNOTATION =
            Attribute.of("com.palantir.idea-language-injector.has-language-annotation", Boolean.class);

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, _javaPlugin -> {
            registerComponentMetadataRules(project);
            createOutgoingConfiguration(project);
        });
    }

    private static void registerComponentMetadataRules(Project project) {
        project.getDependencies().getComponents().all(component -> {
            component.allVariants(variant -> {
                variant.withDependencies(dependencies -> {
                    dependencies.forEach(dep -> {
                        if ("org.jetbrains".equals(dep.getGroup()) && "annotations".equals(dep.getName())) {
                            variant.attributes(attrs -> attrs.attribute(HAS_LANGUAGE_ANNOTATION, true));
                        }
                    });
                });
            });
        });
    }

    private static void createOutgoingConfiguration(Project project) {
        project.getConfigurations().consumable("idea-language-injector-outgoing", outgoing -> {
            outgoing.attributes(attrs -> {
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, OUTGOING_USAGE));
            });

            project.getExtensions().getByType(SourceSetContainer.class).configureEach(sourceSet -> {
                Provider<Set<File>> jarsProvider = project.getConfigurations()
                        .named(sourceSet.getCompileClasspathConfigurationName())
                        .map(conf -> conf.getIncoming()
                                .artifactView(view -> {
                                    view.attributes(attrs -> {
                                        attrs.attribute(HAS_LANGUAGE_ANNOTATION, true);
                                        attrs.attribute(
                                                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                                                ArtifactTypeDefinition.JAR_TYPE);
                                    });
                                })
                                .getFiles()
                                .getFiles());

                outgoing.getOutgoing().artifacts(jarsProvider);
            });
        });
    }
}
