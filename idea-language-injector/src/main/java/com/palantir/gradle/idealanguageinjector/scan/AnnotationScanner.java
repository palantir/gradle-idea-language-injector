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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AnnotationScanner extends ClassVisitor {
    private static final String LANGUAGE_ANNOTATION = "Lorg/intellij/lang/annotations/Language;";
    private static final String CONSTRUCTOR_NAME = "<init>";

    private final List<AnnotationInfo> findings = new ArrayList<>();
    private String currentClassName;
    private boolean isNonStaticInnerClass;

    AnnotationScanner() {
        super(Opcodes.ASM9);
    }

    public final List<AnnotationInfo> getFindings() {
        return findings;
    }

    @Override
    public final void visit(
            int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.currentClassName = name.replace('/', '.');
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public final void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (name.replace('/', '.').equals(currentClassName)) {
            isNonStaticInnerClass = (access & Opcodes.ACC_STATIC) == 0;
        }
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public final MethodVisitor visitMethod(
            int _access, String name, String descriptor, String _signature, String[] _exceptions) {
        return new MethodScanner(name, descriptor);
    }

    private class MethodScanner extends MethodVisitor {
        private final String methodName;
        private final List<String> parameterTypes;

        MethodScanner(String methodName, String descriptor) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
            this.parameterTypes = determineParameterTypes(descriptor);
        }

        private List<String> determineParameterTypes(String descriptor) {
            List<String> allParams = Arrays.stream(Type.getArgumentTypes(descriptor))
                    .map(Type::getClassName)
                    .toList();

            boolean shouldSkipFirstParam = isNonStaticInnerClass && CONSTRUCTOR_NAME.equals(methodName);
            return shouldSkipFirstParam && !allParams.isEmpty() ? allParams.subList(1, allParams.size()) : allParams;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean _visible) {
            if (!LANGUAGE_ANNOTATION.equals(descriptor)) {
                return null;
            }

            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name) && value instanceof String languageValue) {
                        findings.add(new AnnotationInfo(
                                currentClassName, methodName, parameter, languageValue, parameterTypes));
                    }
                }
            };
        }
    }
}
