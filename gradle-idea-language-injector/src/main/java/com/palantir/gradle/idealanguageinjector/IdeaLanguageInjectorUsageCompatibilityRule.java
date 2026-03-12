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

import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.Usage;

/**
 * When the consumer requests {@link IdeaLanguageInjectorProjectPlugin#OUTGOING_USAGE},
 * accept {@link Usage#JAVA_API} and {@link Usage#JAVA_RUNTIME} as compatible.
 * This is needed because external library dependencies (transitive from the consumable's
 * extendsFrom chains) publish standard Java Usage values.
 */
public abstract class IdeaLanguageInjectorUsageCompatibilityRule implements AttributeCompatibilityRule<Usage> {

    @Override
    public final void execute(CompatibilityCheckDetails<Usage> details) {
        String consumerValue = details.getConsumerValue().getName();
        String producerValue = details.getProducerValue().getName();

        if (consumerValue.equals(IdeaLanguageInjectorProjectPlugin.OUTGOING_USAGE)
                && (producerValue.equals(Usage.JAVA_API) || producerValue.equals(Usage.JAVA_RUNTIME))) {
            details.compatible();
        }
    }
}
