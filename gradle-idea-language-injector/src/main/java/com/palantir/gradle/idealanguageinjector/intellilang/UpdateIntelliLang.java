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

package com.palantir.gradle.idealanguageinjector.intellilang;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisableCachingByDefault(because = "Not opting into build caching; explicit opt-out is required by Gradle 9.7")
public abstract class UpdateIntelliLang extends DefaultTask {
    private static final Logger log = LoggerFactory.getLogger(UpdateIntelliLang.class);

    @InputFiles
    public abstract ConfigurableFileCollection getArtifactFiles();

    @OutputFile
    public abstract RegularFileProperty getIntelliLangFile();

    @Inject
    protected abstract ProjectLayout getProjectLayout();

    public UpdateIntelliLang() {
        getIntelliLangFile().set(getProjectLayout().getProjectDirectory().file(".idea/IntelliLang.xml"));
    }

    @TaskAction
    public final void updateXml() {
        File intelliLangFile = getIntelliLangFile().get().getAsFile();

        List<IntelliLangInjection> allInjections = getArtifactFiles().getFiles().stream()
                .map(IntelliLangMapper::read)
                .flatMap(Collection::stream)
                .toList();

        if (allInjections.isEmpty()) {
            log.info("No language injections found. Skipping update.");
            return;
        }

        IntelliLangMapper.write(intelliLangFile, allInjections);
    }
}
