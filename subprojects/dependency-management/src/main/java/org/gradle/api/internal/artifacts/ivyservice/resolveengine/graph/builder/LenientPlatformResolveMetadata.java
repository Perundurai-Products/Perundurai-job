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
package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.builder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.attributes.AttributesSchemaInternal;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.internal.component.external.model.ComponentVariant;
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata;
import org.gradle.internal.component.external.model.ModuleComponentResolveMetadata;
import org.gradle.internal.component.external.model.MutableModuleComponentResolveMetadata;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.internal.hash.HashValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

class LenientPlatformResolveMetadata implements ModuleComponentResolveMetadata {

    private final ModuleComponentIdentifier moduleComponentIdentifier;
    private final ModuleVersionIdentifier moduleVersionIdentifier;
    private final VirtualPlatformState platformState;

    LenientPlatformResolveMetadata(ModuleComponentIdentifier moduleComponentIdentifier, ModuleVersionIdentifier moduleVersionIdentifier, VirtualPlatformState platformState) {
        this.moduleComponentIdentifier = moduleComponentIdentifier;
        this.moduleVersionIdentifier = moduleVersionIdentifier;
        this.platformState = platformState;
    }

    @Override
    public ModuleComponentIdentifier getId() {
        return moduleComponentIdentifier;
    }

    @Override
    public ModuleVersionIdentifier getModuleVersionId() {
        return moduleVersionIdentifier;
    }

    @Override
    public ModuleSource getSource() {
        return null;
    }

    @Override
    public AttributesSchemaInternal getAttributesSchema() {
        return null;
    }

    @Override
    public ModuleComponentResolveMetadata withSource(ModuleSource source) {
        return this;
    }

    @Override
    public Set<String> getConfigurationNames() {
        return null;
    }

    @Nullable
    @Override
    public ConfigurationMetadata getConfiguration(String name) {
        return null;
    }

    @Override
    public Optional<ImmutableList<? extends ConfigurationMetadata>> getVariantsForGraphTraversal() {
        return Optional.absent();
    }

    @Override
    public boolean isMissing() {
        return false;
    }

    @Override
    public boolean isChanging() {
        return false;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public List<String> getStatusScheme() {
        return null;
    }

    @Override
    public ImmutableList<? extends ComponentIdentifier> getPlatformOwners() {
        return ImmutableList.of();
    }

    @Override
    public MutableModuleComponentResolveMetadata asMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModuleComponentArtifactMetadata artifact(String type, @Nullable String extension, @Nullable String classifier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashValue getOriginalContentHash() {
        return null;
    }

    @Override
    public ImmutableList<? extends ComponentVariant> getVariants() {
        return ImmutableList.of();
    }

    @Override
    public ImmutableAttributesFactory getAttributesFactory() {
        return null;
    }

    @Override
    public AttributeContainer getAttributes() {
        return ImmutableAttributes.EMPTY;
    }

    VirtualPlatformState getPlatformState() {
        return platformState;
    }
}
