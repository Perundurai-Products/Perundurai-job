/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.transform;

import org.gradle.api.artifacts.component.ComponentIdentifier;

/**
 * Factory for {@link ExecutionGraphDependenciesResolver} that relies on the information provided to its {@code create} method.
 */
public interface ExtraExecutionGraphDependenciesResolverFactory {
    ExtraExecutionGraphDependenciesResolverFactory ALWAYS_EMPTY_RESOLVER_FACTORY = new ExtraExecutionGraphDependenciesResolverFactory() {
        @Override
        public ExecutionGraphDependenciesResolver create(ComponentIdentifier componentIdentifier, Transformation transformation) {
            return ExecutionGraphDependenciesResolver.EMPTY_RESOLVER;
        }
    };

    /**
     * Creates a {@link ExecutionGraphDependenciesResolver} based on the provided {@code ComponentIdentifier} and {@code Transformation}.
     *
     * @param componentIdentifier the identifier of the artifact under transform
     * @param transformation the transformation definition
     *
     * @return an {@code ExecutionGraphDependenciesResolver} based on the provided parameters
     */
    ExecutionGraphDependenciesResolver create(ComponentIdentifier componentIdentifier, Transformation transformation);
}
