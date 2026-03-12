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

import org.gradle.api.attributes.AttributeDisambiguationRule;
import org.gradle.api.attributes.MultipleCandidatesDetails;
import org.gradle.api.attributes.Usage;

/**
 * When multiple candidates are compatible (both {@link Usage#JAVA_API} and {@link Usage#JAVA_RUNTIME}),
 * prefer {@link Usage#JAVA_API} since we want compile-time dependencies for annotation scanning.
 */
public abstract class IdeaLanguageInjectorUsageDisambiguationRule implements AttributeDisambiguationRule<Usage> {

    @Override
    public final void execute(MultipleCandidatesDetails<Usage> details) {
        if (details.getConsumerValue() != null
                && details.getConsumerValue().getName().equals(IdeaLanguageInjectorProjectPlugin.OUTGOING_USAGE)) {
            for (Usage candidate : details.getCandidateValues()) {
                if (candidate.getName().equals(Usage.JAVA_API)) {
                    details.closestMatch(candidate);
                    return;
                }
            }
        }
    }
}
