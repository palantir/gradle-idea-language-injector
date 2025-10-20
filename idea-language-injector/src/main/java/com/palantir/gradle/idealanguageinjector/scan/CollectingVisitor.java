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
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class CollectingVisitor extends ClassVisitor {
    private final List<IntelliLangInjection> findings = new ArrayList<>();
    private ClassContext context;

    CollectingVisitor() {
        super(Opcodes.ASM9);
    }

    List<IntelliLangInjection> getFindings() {
        return List.copyOf(findings);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.context = new ClassContext(name.replace('/', '.'), false);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if (name.replace('/', '.').equals(context.className())) {
            boolean isNonStatic = (access & Opcodes.ACC_STATIC) == 0;
            this.context = new ClassContext(context.className(), isNonStatic);
        }
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(
            int _access, String name, String descriptor, String _signature, String[] _exceptions) {
        return new MethodScanner(context, name, descriptor, findings::add);
    }

    public record ClassContext(String className, boolean isNonStaticInnerClass) {}
}
