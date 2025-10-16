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

import com.palantir.gradle.idealanguageinjector.scan.AnnotationScanTransform;
import java.io.File;
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

    @InputFiles
    public abstract ConfigurableFileCollection getArtifactFiles();

    @OutputFile
    public abstract RegularFileProperty getIntelliLang();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public UpdateXml() {
        getIntelliLang().set(getProjectLayout().getProjectDirectory().file(".idea/IntelliLang.xml"));
    }

    @TaskAction
    public final void updateXml() {
        File intelliLang = getIntelliLang().get().getAsFile();

        List<Project> allProjects = Stream.concat(
                        Stream.of(IntelliLangXml.read(intelliLang)),
                        getArtifactFiles().getFiles().stream()
                                .flatMap(UpdateXml::findScanFiles)
                                .map(IntelliLangXml::read))
                .toList();

        Project merged = Project.mergeAll(allProjects);

        if (merged.component().injections().isEmpty()) {
            log.info("No language injections found. Skipping update.");
            return;
        }

        IntelliLangXml.write(intelliLang, merged);
    }

    private static Stream<File> findScanFiles(File file) {
        return Optional.ofNullable(
                        file.listFiles((_dir, name) -> name.endsWith(AnnotationScanTransform.LANGUAGE_SCAN_FILE)))
                .stream()
                .flatMap(Stream::of);
    }
}
