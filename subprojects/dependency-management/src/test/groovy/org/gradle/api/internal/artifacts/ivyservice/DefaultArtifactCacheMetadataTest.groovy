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
package org.gradle.api.internal.artifacts.ivyservice

import org.gradle.cache.internal.CacheScopeMapping
import org.gradle.cache.internal.VersionStrategy
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class DefaultArtifactCacheMetadataTest extends Specification {
    @Rule TestNameTestDirectoryProvider temporaryFolder
    def scopeMapping = Stub(CacheScopeMapping)

    def "calculates file store directory"() {
        given:
        TestFile testCacheDir = temporaryFolder.file("test/cache")
        scopeMapping.getBaseDirectory(null, CacheLayout.ROOT.key, VersionStrategy.SharedCache) >> testCacheDir

        when:
        def metaData = new DefaultArtifactCacheMetadata(scopeMapping)
        File fileStore = metaData.getFileStoreDirectory()

        then:
        fileStore == new File(testCacheDir, CacheLayout.FILE_STORE.key)
    }

    def "calculates metadata store directory"() {
        given:
        TestFile testCacheDir = temporaryFolder.file("test/cache")
        scopeMapping.getBaseDirectory(null, CacheLayout.ROOT.key, VersionStrategy.SharedCache) >> testCacheDir

        when:
        def metaData = new DefaultArtifactCacheMetadata(scopeMapping)
        File metadataStore = metaData.getMetaDataStoreDirectory()

        then:
        metadataStore == new File(testCacheDir, CacheLayout.META_DATA.key + '/descriptors')
    }
}
