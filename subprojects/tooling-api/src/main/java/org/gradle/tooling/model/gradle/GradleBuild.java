/*
 * Copyright 2013 the original author or authors.
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

package org.gradle.tooling.model.gradle;

import org.gradle.api.Incubating;
import org.gradle.tooling.model.BuildIdentifier;
import org.gradle.tooling.model.BuildModel;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.Model;

/**
 * Provides information about the structure of a Gradle build.
 *
 * @since 1.8
 */
public interface GradleBuild extends Model, BuildModel {
    /**
     * Returns the identifier for this Gradle build.
     *
     * @since 2.13
     */
    BuildIdentifier getBuildIdentifier();

    /**
     * Returns the root project for this build.
     *
     * @return The root project
     */
    BasicGradleProject getRootProject();

    /**
     * Returns the set of all projects for this build.
     *
     * @return The set of all projects.
     */
    DomainObjectSet<? extends BasicGradleProject> getProjects();

    /**
     * Returns the included builds that were referenced by this build.
     *
     * @since 3.3
     */
    DomainObjectSet<? extends GradleBuild> getIncludedBuilds();

    /**
     * Returns all builds contained in this build, and for which tooling models should be built when importing into an IDE. This is not necessarily the same as {@link #getIncludedBuilds()}, as an included build is not necessarily 'owned' by a build that includes it.
     *
     * <p>For the root build, this set contains all builds that participate in the composite build, including those from all nested included builds. For other builds, this set is empty.</p>
     *
     * @since 4.10
     */
    @Incubating
    DomainObjectSet<? extends GradleBuild> getAllBuilds();
}
