/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.artifacts;

import org.gradle.api.Action;
import org.gradle.api.Incubating;

import javax.annotation.Nullable;

/**
 * Represents a constraints over all, including transitive, dependencies.
 *
 * @since 4.5
 */
@Incubating
public interface DependencyConstraint extends ModuleVersionSelector {

    /**
     * Configures the version constraint for this dependency constraint.
     *
     * @param configureAction the configuration action for the module version
     */
    void version(Action<? super MutableVersionConstraint> configureAction);

    /**
     * Returns a reason why this dependency constraint should be used, in particular with regards to its version. The dependency report will use it to explain why a specific dependency was selected, or why a
     * specific dependency version was used.
     *
     * @return a reason to use this dependency constraint
     * @since 4.6
     */
    @Nullable
    String getReason();

    /**
     * Sets the reason why this dependency constraint should be used.
     *
     * @since 4.6
     */
    void because(String reason);
}
