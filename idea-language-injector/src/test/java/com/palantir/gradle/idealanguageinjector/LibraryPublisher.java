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

package com.palantir.gradle.idealanguageinjector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Test fixture for publishing test libraries to a local Maven repository.
 * Use by adding {@code @ExtendWith(LibraryPublisher.Extension.class)} to your test class.
 */
public final class LibraryPublisher {
    private final Path localRepo;

    private LibraryPublisher(Path localRepo) {
        this.localRepo = localRepo;
    }

    /**
     * Publishes a library by compiling source files and creating a JAR in the local repository.
     *
     * @param artifact the artifact name
     * @param resourceFiles the Java source files to compile (from src/test/resources/test-libraries/)
     * @return the Maven coordinates string (e.g., "com.example:artifact:1.0.0")
     */
    public String publishLibrary(String artifact, String... resourceFiles) throws IOException {
        Path tempDir = Files.createTempDirectory("lib-" + artifact);
        Path srcDir = tempDir.resolve("src");
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);

        // Compile sources
        List<File> sources = new ArrayList<>();
        for (String fileName : resourceFiles) {
            String content = readResource("/test-libraries/" + fileName);
            Matcher pkgMatcher = Pattern.compile("package\\s+([\\w.]+);").matcher(content);
            String pkgPath = pkgMatcher.find() ? pkgMatcher.group(1).replace('.', '/') : "";

            Path srcFile = srcDir.resolve(pkgPath).resolve(fileName);
            Files.createDirectories(srcFile.getParent());
            Files.writeString(srcFile, content);
            sources.add(srcFile.toFile());
        }

        // Compile
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(binDir.toFile()));
            compiler.getTask(
                            null,
                            fm,
                            null,
                            List.of("-cp", System.getProperty("java.class.path")),
                            null,
                            fm.getJavaFileObjectsFromFiles(sources))
                    .call();
        }

        // Create JAR and POM
        Path repoDir = localRepo.resolve("com/example/" + artifact + "/1.0.0");
        Files.createDirectories(repoDir);

        try (JarOutputStream jar =
                        new JarOutputStream(Files.newOutputStream(repoDir.resolve(artifact + "-1.0.0.jar")));
                Stream<Path> stream = Files.walk(binDir)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                jar.putNextEntry(new JarEntry(binDir.relativize(file).toString().replace(File.separatorChar, '/')));
                Files.copy(file, jar);
                jar.closeEntry();
            }
        }

        Files.writeString(
                repoDir.resolve(artifact + "-1.0.0.pom"),
                // language=xml
                """
                <?xml version="1.0"?>
                <project><modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId><artifactId>%s</artifactId><version>1.0.0</version>
                </project>
                """.formatted(artifact));

        return "com.example:" + artifact + ":1.0.0";
    }

    public String mavenRepoUri() {
        return localRepo.toUri().toString();
    }

    private static String readResource(String path) throws IOException {
        try (InputStream is = LibraryPublisher.class.getResourceAsStream(path)) {
            Assertions.assertNotNull(is, "Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** JUnit extension that provides LibraryPublisher as a test parameter. */
    public static final class Extension implements ParameterResolver, BeforeEachCallback {
        private static final String LOCAL_REPO_KEY = "localRepo";

        @Override
        public void beforeEach(ExtensionContext context) throws IOException {
            // Create and store localRepo for each test
            Path localRepo = Files.createTempDirectory("maven-repo");
            context.getStore(ExtensionContext.Namespace.create(Extension.class)).put(LOCAL_REPO_KEY, localRepo);
        }

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext _extensionContext) {
            return parameterContext.getParameter().getType().equals(LibraryPublisher.class);
        }

        @Override
        public Object resolveParameter(ParameterContext _parameterContext, ExtensionContext extensionContext) {
            Path localRepo = extensionContext
                    .getStore(ExtensionContext.Namespace.create(Extension.class))
                    .get(LOCAL_REPO_KEY, Path.class);

            if (localRepo == null) {
                throw new IllegalStateException("localRepo not found in ExtensionContext. "
                        + "This should not happen as it's initialized in beforeEach.");
            }

            return new LibraryPublisher(localRepo);
        }
    }
}
