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
