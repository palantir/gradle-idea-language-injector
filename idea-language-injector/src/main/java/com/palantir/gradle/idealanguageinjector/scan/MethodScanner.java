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

import com.palantir.gradle.idealanguageinjector.intellilang.IntelliLangInjection;
import com.palantir.gradle.idealanguageinjector.scan.CollectingVisitor.ClassContext;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public final class MethodScanner extends MethodVisitor {
    private final ClassContext classContext;
    private final String methodName;
    private final List<String> parameterTypes;
    private final Consumer<IntelliLangInjection> resultConsumer;

    MethodScanner(
            ClassContext classContext,
            String methodName,
            String descriptor,
            Consumer<IntelliLangInjection> resultConsumer) {
        super(Opcodes.ASM9);
        this.classContext = classContext;
        this.methodName = methodName;
        this.parameterTypes = extractParameterTypes(descriptor, classContext, methodName);
        this.resultConsumer = resultConsumer;
    }

    private static List<String> extractParameterTypes(String descriptor, ClassContext context, String methodName) {
        List<String> allParams = Arrays.stream(Type.getArgumentTypes(descriptor))
                .map(Type::getClassName)
                .toList();

        boolean isInnerConstructor = context.isNonStaticInnerClass() && methodName.equals("<init>");

        return isInnerConstructor && !allParams.isEmpty() ? allParams.subList(1, allParams.size()) : allParams;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean _visible) {
        if (!descriptor.equals("Lorg/intellij/lang/annotations/Language;")) {
            return null;
        }

        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String name, Object value) {
                if (!(name.equals("value") && value instanceof String language)) {
                    return;
                }

                resultConsumer.accept(IntelliLangInjection.from(
                        classContext.className(), methodName, parameterTypes, parameter, language));
            }
        };
    }
}
