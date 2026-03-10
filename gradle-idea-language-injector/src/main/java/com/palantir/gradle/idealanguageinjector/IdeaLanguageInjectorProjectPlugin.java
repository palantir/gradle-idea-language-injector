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
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;

public abstract class IdeaLanguageInjectorProjectPlugin implements Plugin<Project> {

    static final String OUTGOING_USAGE = "idea-language-injector-jars";

    @Override
    public final void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, _javaPlugin -> {
            createOutgoingConfiguration(project);
        });
    }

    private static void createOutgoingConfiguration(Project project) {
        project.getConfigurations().consumable("idea-language-injector-outgoing", outgoing -> {
            outgoing.attributes(attrs -> {
                attrs.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, OUTGOING_USAGE));
            });

            outgoing.getOutgoing()
                    .artifacts(project.provider(() -> sourceSetArtifactView(project, ArtifactTypeDefinition.JAR_TYPE)
                            .getFiles()));
        });
    }

    static FileCollection sourceSetArtifactView(Project project, String artifactType) {
        ConfigurableFileCollection files = project.files();
        project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
            files.from(project.getConfigurations()
                    .getByName(sourceSet.getCompileClasspathConfigurationName())
                    .getIncoming()
                    .artifactView(view -> {
                        view.lenient(true);
                        view.attributes(
                                attrs -> attrs.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, artifactType));
                    })
                    .getFiles());
        });
        return files;
    }
}
