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

import java.util.Optional;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.attributes.Usage;

/**
 * Allows configurations requesting {@link IdeaLanguageInjectorProjectPlugin#USAGE_NAME} usage to resolve
 * transitive dependencies that only offer {@link Usage#JAVA_API}. The reverse is not true: consumers
 * requesting {@code JAVA_API} will never match variants offering our custom usage.
 */
public abstract class IdeaLanguageInjectorUsageCompatibilityRule implements AttributeCompatibilityRule<Usage> {

    @Override
    public final void execute(CompatibilityCheckDetails<Usage> details) {
        boolean consumerMatches = Optional.ofNullable(details.getConsumerValue())
                .map(Usage::getName)
                .map(IdeaLanguageInjectorProjectPlugin.OUTGOING_USAGE::equals)
                .orElse(false);

        boolean producerMatches = Optional.ofNullable(details.getProducerValue())
                .map(Usage::getName)
                .map(Usage.JAVA_API::equals)
                .orElse(false);

        if (consumerMatches && producerMatches) {
            details.compatible();
        }
    }
}
