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

package com.palantir.gradle.idealanguageinjector.ideaxml;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.palantir.gradle.idealanguageinjector.scan.AnnotationInfo;
import com.palantir.gradle.idealanguageinjector.scan.ScanTransform;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UpdateXml extends DefaultTask {
    private static final Logger log = LoggerFactory.getLogger(UpdateXml.class);

    private static final ObjectMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory())
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    @InputFiles
    public abstract ConfigurableFileCollection getArtifactFiles();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public UpdateXml() {
        getOutputFile().set(getProjectLayout().getProjectDirectory().file(".idea/IntelliLang.xml"));
    }

    @TaskAction
    public final void updateXml() {
        List<AnnotationInfo> annotationInfos = getArtifactFiles().getFiles().stream()
                .flatMap(UpdateXml::findScanFiles)
                .flatMap(UpdateXml::readAnnotationsFromFile)
                .toList();

        File outputFile = getOutputFile().get().getAsFile();

        if (annotationInfos.isEmpty()) {
            log.info("No language injections found. Skipping update.");
            return;
        }

        List<Injection> addedInjections = toInjections(annotationInfos);
        Project updatedXml = readXml(outputFile)
                .map(existingProject -> mergeInjectionsIntoXml(existingProject, addedInjections))
                .orElseGet(() -> createNewProject(addedInjections));

        writeXml(outputFile, updatedXml);
    }

    private static Stream<File> findScanFiles(File file) {
        return Stream.of(file)
                .flatMap(f -> f.isDirectory()
                        ? Optional.ofNullable(f.listFiles(
                                        (_dir, name) -> name.endsWith(ScanTransform.LANGUAGE_SCAN_FILE)))
                                .stream()
                                .flatMap(Stream::of)
                        : Stream.of(f).filter(f1 -> f1.getName().endsWith(ScanTransform.LANGUAGE_SCAN_FILE)));
    }

    private static Stream<AnnotationInfo> readAnnotationsFromFile(File dataFile) {
        try {
            return AnnotationInfo.readFromFile(dataFile).stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static Optional<Project> readXml(File outputFile) {
        if (!outputFile.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(XML_MAPPER.readValue(outputFile, Project.class));
        } catch (IOException e) {
            log.error("Failed to parse existing configuration file: {}", outputFile, e);
        }
        return Optional.empty();
    }

    private static Project mergeInjectionsIntoXml(
            Project existingProject, List<Injection> newInjections) {

        List<Injection> existingInjections =
                existingProject.component().injections();

        // Use composite key: (displayName, language, injectorId)
        Map<InjectionKey, Injection> mergedMap = Stream.concat(
                        existingInjections.stream(), newInjections.stream())
                .collect(Collectors.toMap(
                        InjectionKey::from,
                        injection -> injection,
                        UpdateXml::mergeInjections,
                        LinkedHashMap::new));

        List<Injection> mergedInjections = mergedMap.values().stream()
                .sorted(Comparator.comparing(Injection::displayName)
                        .thenComparing(Injection::language)
                        .thenComparing(Injection::injectorId))
                .collect(Collectors.toList());

        return Project.of(Component.of(mergedInjections), existingProject.version());
    }

    private static Injection mergeInjections(
            Injection existing, Injection replacement) {
        // Combine places from both injections and remove duplicates
        List<String> mergedPlaces = Stream.concat(existing.places().stream(), replacement.places().stream())
                .map(Place::pattern)
                .distinct()
                .sorted()
                .toList();

        return Injection.builder()
                .from(existing)
                .places(mergedPlaces.stream().map(Place::of).collect(Collectors.toList()))
                .build();
    }

    private static Project createNewProject(List<Injection> injections) {
        // Merge injections with same composite key before creating project
        Map<InjectionKey, Injection> mergedMap = injections.stream()
                .collect(Collectors.toMap(
                        InjectionKey::from,
                        injection -> injection,
                        UpdateXml::mergeInjections,
                        LinkedHashMap::new));

        List<Injection> sortedInjections = mergedMap.values().stream()
                .sorted(Comparator.comparing(Injection::displayName)
                        .thenComparing(Injection::language)
                        .thenComparing(Injection::injectorId))
                .collect(Collectors.toList());
        return Project.of(Component.of(sortedInjections), "4");
    }

    private static List<Injection> toInjections(List<AnnotationInfo> annotationInfos) {
        return annotationInfos.stream().map(Injection::from).collect(Collectors.toList());
    }

    private void writeXml(File outputFile, Project updatedXml) {
        try {
            outputFile.getParentFile().mkdirs();
            XML_MAPPER.writeValue(outputFile, updatedXml);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write back to configuration file: "
                            + getOutputFile().get(),
                    e);
        }
    }

    private record InjectionKey(String displayName, String language, String injectorId) {
        static InjectionKey from(Injection injection) {
            return new InjectionKey(injection.displayName(), injection.language(), injection.injectorId());
        }
    }
}
