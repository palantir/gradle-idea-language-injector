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
import com.palantir.gradle.idealanguageinjector.scan.AnnotationScanTransform;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
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
        List<Injection> addedInjections = getArtifactFiles().getFiles().stream()
                .flatMap(UpdateXml::findScanFiles)
                .map(UpdateXml::readXml)
                .flatMap(project -> project.component().injections().stream())
                .toList();

        File outputFile = getOutputFile().get().getAsFile();

        if (addedInjections.isEmpty()) {
            log.info("No language injections found. Skipping update.");
            return;
        }

        writeXml(outputFile, createOrMergeProject(readXml(outputFile), addedInjections));
    }

    private static Stream<File> findScanFiles(File file) {
        return Stream.of(file)
                .flatMap(f -> f.isDirectory()
                        ? Optional.ofNullable(f.listFiles(
                                        (_dir, name) -> name.endsWith(AnnotationScanTransform.LANGUAGE_SCAN_FILE)))
                                .stream()
                                .flatMap(Stream::of)
                        : Stream.of(f).filter(f1 -> f1.getName().endsWith(AnnotationScanTransform.LANGUAGE_SCAN_FILE)));
    }

    private static Project readXml(File file) {
        if (!file.exists()) {
            return Project.empty();
        }
        try {
            return Optional.ofNullable(XML_MAPPER.readValue(file, Project.class))
                    .orElseGet(Project::empty);
        } catch (IOException e) {
            log.error("Failed to parse existing configuration file: {}", file, e);
            return Project.empty();
        }
    }

    private static Project createOrMergeProject(Project existing, List<Injection> newInjections) {
        List<Injection> allInjections = Stream.concat(
                        existing.component().injections().stream(), newInjections.stream())
                .toList();
        return Project.of(Component.of(allInjections), existing.version());
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
}
