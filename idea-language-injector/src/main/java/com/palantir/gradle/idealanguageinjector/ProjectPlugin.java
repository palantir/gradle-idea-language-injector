package com.palantir.gradle.idealanguageinjector;

import com.palantir.gradle.idealanguageinjector.scan.AnnotationScanTransform;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;

public final class ProjectPlugin implements Plugin<Project> {

    static final String LANGUAGE_ANNOTATION_SCANS = "language-annotation-scans";
    private static final Attribute<Boolean> HAS_LANGUAGE_ANNOTATION =
            Attribute.of("has-language-annotation", Boolean.class);

    @Override
    public void apply(Project project) {
        project.getPlugins().withType(JavaPlugin.class, _javaPlugin -> {
            registerComponentMetadataRules(project);
            registerTransform(project);
            TaskProvider<Sync> collectTask = createCollectTask(project);
            createOutgoingConfiguration(project, collectTask);
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

    private static void registerTransform(Project project) {
        project.getDependencies().registerTransform(AnnotationScanTransform.class, spec -> {
            spec.getFrom()
                    .attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
                    .attribute(HAS_LANGUAGE_ANNOTATION, true);
            spec.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, LANGUAGE_ANNOTATION_SCANS);
        });
    }

    private static TaskProvider<Sync> createCollectTask(Project project) {
        return project.getTasks().register("collectLanguageScans", Sync.class, task -> {
            project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
                task.from(project.getConfigurations()
                        .named(sourceSet.getCompileClasspathConfigurationName())
                        .map(conf -> conf.getIncoming()
                                .artifactView(view -> {
                                    view.lenient(true);
                                    view.attributes(attrs -> attrs.attribute(
                                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, LANGUAGE_ANNOTATION_SCANS));
                                })
                                .getFiles()));
            });

            DirectoryProperty outputDir = project.getObjects().directoryProperty();
            outputDir.set(project.getLayout().getBuildDirectory().dir("annotation-scans-output"));
            task.into(outputDir);
        });
    }

    private static void createOutgoingConfiguration(Project project, TaskProvider<Sync> collectTask) {
        project.getConfigurations().consumable(LANGUAGE_ANNOTATION_SCANS, outgoing -> {
            outgoing.attributes(attrs -> {
                attrs.attribute(
                        Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, LANGUAGE_ANNOTATION_SCANS));
            });
            outgoing.getOutgoing().artifact(collectTask, artifact -> artifact.builtBy(collectTask));
        });
    }
}
