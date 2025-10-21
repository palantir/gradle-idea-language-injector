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
package com.example.complex;

import java.util.List;
import org.intellij.lang.annotations.Language;

public class ComplexLib {

    public ComplexLib(@Language("JSON") String config) {}

    public ComplexLib(int timeout, @Language("XML") String query) {}

    public void execute(@Language("SQL") String query1, @Language("SQL") String query2) {}

    public void queryVarargs(@Language("SQL") String sql, Object... args) {}

    public void queryWithList(@Language("XML") String xml, List<String> items) {}

    public void queryWithArray(@Language("XML") String xml, int[] ids) {}

    public static void executeStatic(@Language("HTML") String html) {}

    public void process(@Language("JSON") String json) {}

    public void process(@Language("JSON") String json, int timeout) {}

    public static class Nested {
        public Nested(@Language("JSON") String pattern) {}

        public void compile(@Language("XML") String xml) {}
    }

    public class Inner {
        public Inner(@Language("HTML") String template) {}

        public void render(@Language("HTML") String html) {}
    }
}
