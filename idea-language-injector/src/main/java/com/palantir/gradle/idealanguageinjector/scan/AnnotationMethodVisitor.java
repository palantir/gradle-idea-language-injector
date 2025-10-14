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

import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AnnotationMethodVisitor extends MethodVisitor {
    private static final String LANGUAGE_ANNOTATION = "Lorg/intellij/lang/annotations/Language;";
    private static final String CONSTRUCTOR_NAME = "<init>";

    private final String className;
    private final String methodName;
    private final List<String> parameterTypes;
    private final List<AnnotationInfo> findings;

    AnnotationMethodVisitor(
            String className,
            String methodName,
            String descriptor,
            List<AnnotationInfo> findings,
            boolean isNonStaticInnerClass) {
        super(Opcodes.ASM9);
        this.className = className;
        this.methodName = methodName;
        this.findings = findings;
        this.parameterTypes = determineParameterTypes(descriptor, methodName, isNonStaticInnerClass);
    }

    private static List<String> determineParameterTypes(
            String descriptor, String methodName, boolean isNonStaticInnerClass) {
        List<String> allParams = Arrays.stream(Type.getArgumentTypes(descriptor))
                .map(Type::getClassName)
                .toList();

        boolean shouldSkipFirstParam = isNonStaticInnerClass && CONSTRUCTOR_NAME.equals(methodName);
        return shouldSkipFirstParam && !allParams.isEmpty() ? allParams.subList(1, allParams.size()) : allParams;
    }

    @Override
    public final AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean _visible) {
        if (!LANGUAGE_ANNOTATION.equals(descriptor)) {
            return null;
        }

        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if ("value".equals(name) && value instanceof String languageValue) {
                    findings.add(new AnnotationInfo(
                            className, methodName, parameter, languageValue, parameterTypes));
                }
            }
        };
    }
}
