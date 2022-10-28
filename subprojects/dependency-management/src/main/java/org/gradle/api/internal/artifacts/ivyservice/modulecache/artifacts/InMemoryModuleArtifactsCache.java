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
package org.gradle.api.internal.artifacts.ivyservice.modulecache.artifacts;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleComponentRepository;
import org.gradle.internal.component.model.ComponentArtifactMetadata;
import org.gradle.util.BuildCommencedTimeProvider;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryModuleArtifactsCache implements ModuleArtifactsCache {
    protected final BuildCommencedTimeProvider timeProvider;
    private final Map<ArtifactsAtRepositoryKey, ModuleArtifactsCacheEntry> inMemoryCache = Maps.newConcurrentMap();

    public InMemoryModuleArtifactsCache(BuildCommencedTimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public CachedArtifacts cacheArtifacts(ModuleComponentRepository repository, ComponentIdentifier componentId, String context, BigInteger descriptorHash, Collection<? extends ComponentArtifactMetadata> artifacts) {
        ArtifactsAtRepositoryKey key = new ArtifactsAtRepositoryKey(repository.getId(), componentId, context);
        ModuleArtifactsCacheEntry entry = new ModuleArtifactsCacheEntry(ImmutableSet.copyOf(artifacts), timeProvider.getCurrentTime(), descriptorHash);
        store(key, entry);
        return createCacheArtifacts(entry);
    }

    protected void store(ArtifactsAtRepositoryKey key, ModuleArtifactsCacheEntry entry) {
        inMemoryCache.put(key, entry);
    }

    public CachedArtifacts getCachedArtifacts(ModuleComponentRepository repository, ComponentIdentifier componentId, String context) {
        ArtifactsAtRepositoryKey key = new ArtifactsAtRepositoryKey(repository.getId(), componentId, context);
        ModuleArtifactsCacheEntry entry = get(key);
        return entry == null ? null : createCacheArtifacts(entry);
    }

    protected ModuleArtifactsCacheEntry get(ArtifactsAtRepositoryKey key) {
        return inMemoryCache.get(key);
    }

    private CachedArtifacts createCacheArtifacts(ModuleArtifactsCacheEntry entry) {
        long entryAge = timeProvider.getCurrentTime() - entry.createTimestamp;
        return new DefaultCachedArtifacts(entry.artifacts, entry.moduleDescriptorHash, entryAge);
    }

    protected static class ModuleArtifactsCacheEntry {
        protected final Set<ComponentArtifactMetadata> artifacts;
        protected final BigInteger moduleDescriptorHash;
        protected final long createTimestamp;

        ModuleArtifactsCacheEntry(Set<? extends ComponentArtifactMetadata> artifacts, long createTimestamp, BigInteger moduleDescriptorHash) {
            this.artifacts = new LinkedHashSet<ComponentArtifactMetadata>(artifacts);
            this.createTimestamp = createTimestamp;
            this.moduleDescriptorHash = moduleDescriptorHash;
        }
    }
}
