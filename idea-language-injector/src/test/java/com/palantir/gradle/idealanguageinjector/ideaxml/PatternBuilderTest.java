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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

public class PatternBuilderTest {

    @Test
    void build_display_name_with_simple_class() {
        String displayName = PatternBuilder.buildDisplayName("com.example.TestClass");

        assertThat(displayName).isEqualTo("TestClass (com.example)");
    }

    @Test
    void build_display_name_with_inner_class() {
        String displayName = PatternBuilder.buildDisplayName("com.example.OuterClass$InnerClass");

        assertThat(displayName).isEqualTo("InnerClass (com.example.OuterClass)");
    }

    @Test
    void build_display_name_with_nested_inner_class() {
        String displayName = PatternBuilder.buildDisplayName("com.example.Outer$Middle$Inner");

        assertThat(displayName).isEqualTo("Inner (com.example.Outer.Middle)");
    }

    @Test
    void build_display_name_with_no_package() {
        String displayName = PatternBuilder.buildDisplayName("TestClass");

        assertThat(displayName).isEqualTo("TestClass ()");
    }

    @Test
    void build_pattern_string_for_regular_method() {
        String pattern = PatternBuilder.buildPatternString(
                "com.example.TestClass", "query", List.of("java.lang.String", "int"), 0);

        assertThat(pattern)
                .isEqualTo(
                        "psiParameter().ofMethod(0, psiMethod().withName(\"query\").withParameters(\"java.lang.String\", \"int\").definedInClass(\"com.example.TestClass\"))");
    }

    @Test
    void build_pattern_string_for_constructor() {
        String pattern = PatternBuilder.buildPatternString(
                "com.example.TestClass", "<init>", List.of("java.lang.String"), 0);

        assertThat(pattern)
                .isEqualTo(
                        "psiParameter().ofMethod(0, psiMethod().withName(\"TestClass\").withParameters(\"java.lang.String\").definedInClass(\"com.example.TestClass\"))");
    }

    @Test
    void build_pattern_string_for_inner_class_constructor() {
        String pattern = PatternBuilder.buildPatternString(
                "com.example.Outer$Inner", "<init>", List.of("java.lang.String"), 0);

        assertThat(pattern)
                .isEqualTo(
                        "psiParameter().ofMethod(0, psiMethod().withName(\"Inner\").withParameters(\"java.lang.String\").definedInClass(\"com.example.Outer.Inner\"))");
    }

    @Test
    void build_pattern_string_with_no_parameters() {
        String pattern =
                PatternBuilder.buildPatternString("com.example.TestClass", "execute", List.of(), 0);

        assertThat(pattern)
                .isEqualTo(
                        "psiParameter().ofMethod(0, psiMethod().withName(\"execute\").withParameters().definedInClass(\"com.example.TestClass\"))");
    }

    @Test
    void build_pattern_string_with_multiple_parameters() {
        String pattern = PatternBuilder.buildPatternString(
                "com.example.TestClass",
                "query",
                List.of("java.lang.String", "int", "boolean", "java.util.List"),
                2);

        assertThat(pattern)
                .isEqualTo(
                        "psiParameter().ofMethod(2, psiMethod().withName(\"query\").withParameters(\"java.lang.String\", \"int\", \"boolean\", \"java.util.List\").definedInClass(\"com.example.TestClass\"))");
    }

    @Test
    void build_pattern_string_normalizes_inner_class_delimiters() {
        String pattern = PatternBuilder.buildPatternString(
                "com.example.Outer$Inner$Deep", "method", List.of(), 0);

        assertThat(pattern).contains("definedInClass(\"com.example.Outer.Inner.Deep\")");
    }
}
