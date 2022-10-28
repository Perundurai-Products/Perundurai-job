/*
 * Copyright 2015 the original author or authors.
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
package org.gradle.internal

import org.gradle.test.fixtures.concurrent.ConcurrentSpec
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.SetSystemProperties
import org.junit.Rule

class SystemPropertiesIntegrationTest extends ConcurrentSpec {
    @Rule SetSystemProperties properties = new SetSystemProperties()
    @Rule TestNameTestDirectoryProvider temporaryFolder

    def "creates Java compiler for mismatching Java home directory for multiple threads concurrently"() {
        final int threadCount = 100
        Factory<String> factory = Mock()
        String factoryCreationResult = 'test'
        File originalJavaHomeDir = SystemProperties.instance.javaHomeDir
        File providedJavaHomeDir = temporaryFolder.file('my/test/java/home/toolprovider')

        when:
        async {
            threadCount.times {
                start {
                    String expectedString = SystemProperties.instance.withJavaHome(providedJavaHomeDir, factory)
                    assert factoryCreationResult == expectedString
                }
            }
        }

        then:
        threadCount * factory.create() >> {
            assert SystemProperties.instance.javaHomeDir == providedJavaHomeDir
            factoryCreationResult
        }
        assert SystemProperties.instance.javaHomeDir == originalJavaHomeDir
    }

    def "sets a system property for the duration of a Factory operation"() {
        final int threadCount = 100
        String presetFactoryCreationResult = 'presetTest'
        String notsetFactoryCreationResult = "notsetTest"
        final String notsetPropertyName = "org.gradle.test.sysprop.without.value"
        final String presetPropertyName = "org.gradle.test.sysprop.with.original"
        System.setProperty(presetPropertyName, "original")

        when:
        async {
            threadCount.times { i ->
                def nextValue = i.toString()
                start {
                    Factory<String> factory = new Factory<String>() {
                        @Override
                        String create() {
                            assert System.getProperty(presetPropertyName) == nextValue
                            return presetFactoryCreationResult
                        }
                    }
                    assert SystemProperties.instance.withSystemProperty(presetPropertyName, nextValue, factory) == presetFactoryCreationResult

                    factory = new Factory<String>() {
                        @Override
                        String create() {
                            assert System.getProperty(notsetPropertyName) == nextValue
                            return notsetFactoryCreationResult
                        }
                    }
                    assert SystemProperties.instance.withSystemProperty(notsetPropertyName, nextValue, factory) == notsetFactoryCreationResult
                }
            }
        }

        then:
        assert System.getProperty(presetPropertyName) == "original"
        assert System.getProperty(notsetPropertyName) == null
    }
}
