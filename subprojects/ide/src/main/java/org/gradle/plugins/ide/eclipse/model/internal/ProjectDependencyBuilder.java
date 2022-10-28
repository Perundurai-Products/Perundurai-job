/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.plugins.ide.eclipse.model.internal;

import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.plugins.ide.eclipse.internal.EclipseProjectMetadata;
import org.gradle.plugins.ide.eclipse.model.ProjectDependency;
import org.gradle.plugins.ide.internal.IdeArtifactRegistry;

public class ProjectDependencyBuilder {
    private final IdeArtifactRegistry ideArtifactRegistry;

    public ProjectDependencyBuilder(IdeArtifactRegistry ideArtifactRegistry) {
        this.ideArtifactRegistry = ideArtifactRegistry;
    }

    public ProjectDependency build(ProjectComponentIdentifier id) {
        return buildProjectDependency(determineTargetProjectPath(id));
    }

    private String determineTargetProjectPath(ProjectComponentIdentifier id) {
        return "/" + determineTargetProjectName(id);
    }

    public String determineTargetProjectName(ProjectComponentIdentifier id) {
        EclipseProjectMetadata eclipseProject = ideArtifactRegistry.getIdeProject(EclipseProjectMetadata.class, id);
        return eclipseProject == null ? id.getProjectName() : eclipseProject.getName();
    }

    private ProjectDependency buildProjectDependency(String path) {
        final ProjectDependency out = new ProjectDependency(path);
        out.setExported(false);
        return out;
    }
}
