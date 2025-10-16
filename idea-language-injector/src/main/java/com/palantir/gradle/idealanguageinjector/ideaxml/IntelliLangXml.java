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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IntelliLangXml {
    private static final Logger log = LoggerFactory.getLogger(IntelliLangXml.class);

    private static final ObjectMapper XML_MAPPER = new XmlMapper(new WstxInputFactory(), new WstxOutputFactory())
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private IntelliLangXml() {}

    public static Project read(File file) {
        if (!file.exists()) {
            return Project.empty();
        }
        try {
            return Optional.ofNullable(XML_MAPPER.readValue(file, Project.class))
                    .orElseGet(Project::empty);
        } catch (IOException e) {
            log.error("Failed to parse file: {}", file, e);
            return Project.empty();
        }
    }

    public static void write(File file, Project project) {
        try {
            file.getParentFile().mkdirs();
            XML_MAPPER.writeValue(file, project);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write xml", e);
        }
    }
}
