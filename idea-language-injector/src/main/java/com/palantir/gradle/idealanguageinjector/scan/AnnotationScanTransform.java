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
package com.palantir.gradle.idealanguageinjector.scan;

import com.palantir.gradle.idealanguageinjector.ideaxml.Component;
import com.palantir.gradle.idealanguageinjector.ideaxml.Injection;
import com.palantir.gradle.idealanguageinjector.ideaxml.IntelliLangXml;
import com.palantir.gradle.idealanguageinjector.ideaxml.Project;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.inject.Inject;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters.None;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.objectweb.asm.ClassReader;

public abstract class AnnotationScanTransform implements TransformAction<None> {
    public static final String LANGUAGE_SCAN_FILE = "language-annotations.injections.xml";

    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Inject
    public AnnotationScanTransform() {}

    @Override
    public final void transform(TransformOutputs outputs) {
        File jarFile = getInputArtifact().get().getAsFile();
        List<Injection> injections = scanJarForInjections(jarFile);

        if (!injections.isEmpty()) {
            IntelliLangXml.write(
                    outputs.file(stripJarExtension(jarFile.getName()) + "-" + LANGUAGE_SCAN_FILE),
                    Project.of(Component.of(injections)));
        }
    }

    private List<Injection> scanJarForInjections(File jarFile) {
        try (ZipFile zip = new ZipFile(jarFile)) {
            return zip.stream()
                    .filter(AnnotationScanTransform::isClassFile)
                    .flatMap(entry -> scanClass(zip, entry))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static boolean isClassFile(ZipEntry entry) {
        return !entry.isDirectory() && entry.getName().endsWith(".class");
    }

    public static Stream<Injection> scanClass(ZipFile zip, ZipEntry entry) {
        try (InputStream input = zip.getInputStream(entry)) {
            CollectingVisitor visitor = new CollectingVisitor();
            new ClassReader(input)
                    .accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.getFindings().stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private static String stripJarExtension(String fileName) {
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }
}
