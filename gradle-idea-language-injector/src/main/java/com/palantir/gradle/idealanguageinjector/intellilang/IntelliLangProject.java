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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import org.immutables.value.Value;

/**
 * Root element for IntelliLang.xml configuration file.
 */
@Value.Immutable
@JacksonXmlRootElement(localName = "project")
@JsonDeserialize(as = ImmutableIntelliLangProject.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface IntelliLangProject {

    @JacksonXmlProperty(isAttribute = true)
    @Value.Default
    default String version() {
        return "4";
    }

    @JacksonXmlProperty(localName = "component")
    IntelliLangComponent component();

    static IntelliLangProject of(IntelliLangComponent component) {
        return ImmutableIntelliLangProject.builder().component(component).build();
    }

    static IntelliLangProject empty() {
        return IntelliLangProject.of(IntelliLangComponent.of(List.of()));
    }
}
