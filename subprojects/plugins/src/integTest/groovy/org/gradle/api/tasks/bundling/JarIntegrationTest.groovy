/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.tasks.bundling

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.archives.TestReproducibleArchives
import org.gradle.test.fixtures.archive.JarTestFixture
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import spock.lang.Issue
import spock.lang.Unroll

import java.util.jar.JarFile
import java.util.jar.Manifest

@TestReproducibleArchives
class JarIntegrationTest extends AbstractIntegrationSpec {

    def canCreateAnEmptyJar() {
        given:
        buildFile << """
        task jar(type: Jar) {
            from 'test'
            destinationDir = buildDir
            archiveName = 'test.jar'
        }
        """

        when:
        run 'jar'

        then:
        def jar = new JarTestFixture(file('build/test.jar'))
        jar.assertFileContent('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\n\r\n')
    }

    def canCreateAJarArchiveWithDefaultManifest() {
        given:
        createDir('test') {
            dir1 {
                file 'file1.txt'
            }
        }
        createDir('meta-inf') {
            file 'file1.txt'
            dir2 {
                file 'file2.txt'
            }
        }
        and:
        buildFile << """
            task jar(type: Jar) {
                from 'test'
                metaInf {
                    from 'meta-inf'
                }
                destinationDir = buildDir
                archiveName = 'test.jar'
            }
        """

        when:
        run 'jar'

        then:
        def jar = new JarTestFixture(file('build/test.jar'))
        jar.assertContainsFile('META-INF/file1.txt')
        jar.assertContainsFile('META-INF/dir2/file2.txt')
        jar.assertContainsFile('dir1/file1.txt')
    }

    def "manifest is the first file in the Jar"() {
        given:
        createDir('meta-inf') {
            file('AAA.META') << 'Some custom metadata'
        }
        buildFile << """
            task jar(type: Jar) {
                metaInf {
                    from 'meta-inf'
                }
                destinationDir = buildDir
                archiveName = 'test.jar'
            }
        """

        when:
        succeeds 'jar'

        then:
        def jar = new JarTestFixture(file('build/test.jar'))
        jar.assertContainsFile('META-INF/AAA.META')
    }

    def metaInfSpecsAreIndependentOfOtherSpec() {
        given:
        createDir('test') {
            dir1 {
                file 'ignored.xml'
                file 'file1.txt'
            }
        }
        createDir('meta-inf') {
            dir2 {
                file 'ignored.txt'
                file 'file2.xml'
            }
        }
        createDir('meta-inf2') {
            file 'file2.txt'
            file 'file2.xml'
        }
        and:
        buildFile << """
            task jar(type: Jar) {
                from 'test'
                include '**/*.txt'
                metaInf {
                    from 'meta-inf'
                    include '**/*.xml'
                }
                metaInf {
                    from 'meta-inf2'
                    into 'dir3'
                }
                destinationDir = buildDir
                archiveName = 'test.jar'
            }
        """

        when:
        run 'jar'

        then:
        def jar = new JarTestFixture(file('build/test.jar'))
        jar.assertContainsFile('META-INF/MANIFEST.MF')
        jar.assertContainsFile('META-INF/dir2/file2.xml')
        jar.assertContainsFile('META-INF/dir3/file2.txt')
        jar.assertContainsFile('META-INF/dir3/file2.xml')
        jar.assertContainsFile('dir1/file1.txt')
    }

    def usesManifestFromJarTaskWhenMergingJars() {
        given:
        createDir('src1') {
            dir1 { file 'file1.txt' }
        }
        createDir('src2') {
            dir2 { file 'file2.txt' }
        }
        buildFile << '''
            task jar1(type: Jar) {
                from 'src1'
                destinationDir = buildDir
                archiveName = 'test1.zip'
                manifest { attributes(attr: 'jar1') }
            }
            task jar2(type: Jar) {
                from 'src2'
                destinationDir = buildDir
                archiveName = 'test2.zip'
                manifest { attributes(attr: 'jar2') }
            }
            task jar(type: Jar) {
                dependsOn jar1, jar2
                from zipTree(jar1.archivePath), zipTree(jar2.archivePath)
                manifest { attributes(attr: 'value') }
                destinationDir = buildDir
                archiveName = 'test.jar'
            }
            '''

        when:
        run 'jar'

        then:
        def jar = file('build/test.jar')
        def manifest = jar.manifest
        manifest.mainAttributes.getValue('attr') == 'value'

        def jarFixture = new JarTestFixture(jar)
        jarFixture.assertContainsFile('META-INF/MANIFEST.MF')
        jarFixture.assertContainsFile('dir2/file2.txt')
        jarFixture.assertContainsFile('dir2/file2.txt')
    }

    def excludeDuplicatesUseManifestOverMetaInf() {
        createDir('meta-inf') {
            file 'MANIFEST.MF'
        }
        buildFile << '''
        task jar(type: Jar) {
            duplicatesStrategy = 'exclude'
            metaInf {
                from 'meta-inf'
            }
            manifest {
                attributes(attr: 'from manifest')
            }
            destinationDir = buildDir
            archiveName = 'test.jar'
        }

        '''

        when:
        run 'jar'

        then:
        def jar = file('build/test.jar')
        def manifest = jar.manifest
        manifest.mainAttributes.getValue('attr') == 'from manifest'
    }

    def excludeDuplicatesUseMetaInfOverRegularFiles() {
        createDir('meta-inf1') {
            file 'file.txt'
        }

        createDir('meta-inf2') {
            file 'file.txt'
        }

        file('meta-inf1/file.txt').text = 'good'
        file('meta-inf2/file.txt').text = 'bad'


        buildFile << '''
        task jar(type: Jar) {
            duplicatesStrategy = 'exclude'
            // this should be excluded even though it comes first
            into('META-INF') {
                from 'meta-inf2'
            }
            metaInf {
                from 'meta-inf1'
            }
            destinationDir = buildDir
            archiveName = 'test.jar'
        }

        '''

        when:
        run 'jar'

        then:
        def jar = new JarTestFixture(file('build/test.jar'))
        jar.assertFileContent('META-INF/file.txt', 'good')
    }

    def duplicateServicesIncludedOthersExcluded() {
        createParallelDirsWithServices()

        given:
        buildFile << '''
        task jar(type: Jar) {
            archiveName = 'test.jar'
            destinationDir = projectDir
            from 'dir1'
            from 'dir2'
            eachFile {
                it.duplicatesStrategy = it.relativePath.toString().startsWith('META-INF/services/') ? 'include' : 'exclude'
            }
        }
        '''

        when:
        run 'jar'

        then:
        confirmDuplicateServicesPreserved()
    }

    def duplicatesExcludedByDefaultWithExceptionForServices() {
        createParallelDirsWithServices()

        given:
        buildFile << '''
        task jar(type: Jar) {
            archiveName = 'test.jar'
            destinationDir = projectDir
            from 'dir1'
            from 'dir2'
            duplicatesStrategy = 'exclude'
            filesMatching ('META-INF/services/**') {
                duplicatesStrategy = 'include'
            }
        }
        '''

        when:
        run 'jar'

        then:
        confirmDuplicateServicesPreserved()
    }

    def "changes to manifest attributes should be honoured by incremental build"() {
        given:
        def jarWithManifest = { manifest ->
            """
            task jar(type: Jar) {
                from 'test'
                destinationDir = buildDir
                archiveName = 'test.jar'
                manifest { $manifest }
            }"""
        }

        createDir('test') {
            dir1 {
                file 'file1.txt'
            }
        }
        def jar = file('build/test.jar')

        when:
        buildFile.text = jarWithManifest("")
        run 'jar'

        then:
        jar.manifest.mainAttributes.getValue('attr') == null

        when: "Attribute added"
        buildFile.text = jarWithManifest("attributes(attr: 'Hello')")
        run 'jar'

        then:
        executedAndNotSkipped ':jar'
        jar.manifest.mainAttributes.getValue('attr') == 'Hello'

        when: "Attribute modified"
        buildFile.text = jarWithManifest("attributes(attr: 'Hi')")
        run 'jar'

        then:
        executedAndNotSkipped ':jar'
        jar.manifest.mainAttributes.getValue('attr') == 'Hi'

        when: "Attribute removed"
        buildFile.text = jarWithManifest("")
        run 'jar'

        then:
        executedAndNotSkipped ':jar'
        jar.manifest.mainAttributes.getValue('attr') == null
    }

    private def createParallelDirsWithServices() {
        createDir('dir1') {
            'META-INF' {
                services {
                    file('org.gradle.Service')
                }
            }
            path {
                file 'test.txt'
            }
        }
        createDir('dir2') {
            'META-INF' {
                services {
                    file('org.gradle.Service')
                }
            }
            file {
                file 'test.txt'
            }
        }

        file('dir1/META-INF/services/org.gradle.Service').write('org.gradle.DefaultServiceImpl')
        file('dir2/META-INF/services/org.gradle.Service').write('org.gradle.BetterServiceImpl')
        file('dir1/test.txt').write('Content of first file')
        file('dir2/test.txt').write('Content of second file')
    }

    private def confirmDuplicateServicesPreserved() {
        def jar = new JarTestFixture(file('test.jar'))

        assert 2 == jar.countFiles('META-INF/services/org.gradle.Service')
        assert 1 == jar.countFiles('path/test.txt')

        jar.assertFileContent('test.txt', 'Content of first file')
        jar.hasService('org.gradle.Service', 'org.gradle.BetterServiceImpl')
        jar.hasService('org.gradle.Service', 'org.gradle.DefaultServiceImpl')
    }

    // Only works on Java 8, see https://bugs.openjdk.java.net/browse/JDK-7050570
    @Requires(TestPrecondition.JDK8_OR_LATER)
    @Issue(['GRADLE-1506'])
    def "create Jar with metadata encoded using UTF-8 when platform default charset is not UTF-8"() {
        given:
        buildScript """
            task jar(type: Jar) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
            }
        """.stripIndent()

        createDir('test') {
            // Use an UTF-8 caution symbol in file name
            // that will create a mojibake if encoded using another charset
            file 'mojibake???.txt'
        }

        when:
        executer.withDefaultCharacterEncoding('windows-1252').withTasks("jar")
        executer.run()

        then:
        def jar = new JarTestFixture(file('dest/test.jar'))
        jar.assertContainsFile('mojibake???.txt')
    }

    @Issue('GRADLE-1506')
    def "create Jar with metadata encoded using user supplied charset"() {
        given:
        buildScript """
            task jar(type: Jar) {
                metadataCharset = 'ISO-8859-15'
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
            }
        """.stripIndent()

        createDir('test') {
            file 'mojibak???.txt'
        }
        assert new String('mojibak???.txt').getBytes('ISO-8859-15') != new String('mojibak???.txt').getBytes('UTF-8')

        when:
        succeeds 'jar'

        then:
        def jar = new JarTestFixture(file('dest/test.jar'), 'ISO-8859-15')
        jar.assertContainsFile('mojibak???.txt')
    }

    @Issue('GRADLE-3374')
    def "write manifest encoded using UTF-8 when platform default charset is not UTF-8"() {
        given:
        buildScript """
            task jar(type: Jar) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
                manifest {
                    // Use an UTF-8 caution symbol in manifest entry
                    // that will create a mojibake if encoded using another charset
                    attributes 'moji': 'bake???'
                }
            }
        """.stripIndent()

        when:
        executer.withDefaultCharacterEncoding('windows-1252').withTasks('jar')
        executer.run()

        then:
        def manifest = new JarTestFixture(file('dest/test.jar'), 'UTF-8', 'UTF-8').content('META-INF/MANIFEST.MF')
        manifest.contains('moji: bake???')
    }

    @Issue("GRADLE-3374")
    def "merge manifest read using UTF-8 by default"() {
        given:
        buildScript """
            task jar(type: Jar) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
                manifest {
                    from('manifest-UTF-8.txt')
                }
            }
        """.stripIndent()
        file('manifest-UTF-8.txt').setText('moji: bak???', 'UTF-8')

        when:
        executer.withDefaultCharacterEncoding('ISO-8859-15').withTasks('jar')
        executer.run()

        then:
        def jar = new JarTestFixture(file('dest/test.jar'), 'UTF-8', 'UTF-8')
        def manifest = jar.content('META-INF/MANIFEST.MF')
        manifest.contains('moji: bak???')
    }

    @Issue('GRADLE-3374')
    def "write manifests using a user defined character set"() {
        given:
        buildScript """
            task jar(type: Jar) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
                manifestContentCharset = 'ISO-8859-15'
                manifest {
                    attributes 'moji': 'bak???'
                }
            }
        """.stripIndent()

        when:
        executer.withDefaultCharacterEncoding('UTF-8').withTasks('jar')
        executer.run()

        then:
        def jar = new JarTestFixture(file('dest/test.jar'), 'UTF-8', 'ISO-8859-15')
        def manifest = jar.content('META-INF/MANIFEST.MF')
        manifest.contains('moji: bak???')
    }

    @Issue('GRADLE-3374')
    def "merge manifests using user defined character sets"() {
        given:
        buildScript """
            task jar(type: Jar) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
                manifest {
                    attributes 'moji': 'bak???'
                    from('manifest-ISO-8859-15.txt') {
                        // Charset used to decode the read manifest content
                        contentCharset = 'ISO-8859-15'
                    }
                }
            }
        """.stripIndent()
        file('manifest-ISO-8859-15.txt').setText('bake: moji???', 'ISO-8859-15')

        when:
        executer.withDefaultCharacterEncoding('windows-1252').withTasks('jar')
        executer.run()

        then:
        def jar = new JarTestFixture(file('dest/test.jar'), 'UTF-8', 'UTF-8')
        def manifest = jar.content('META-INF/MANIFEST.MF')
        manifest.contains('moji: bak???')
        manifest.contains('bake: moji???')
    }

    @Issue('GRADLE-3374')
    @Unroll
    def "can merge manifests containing split multi-byte chars using #taskType task"() {
        // Note that there's no need to cover this case with merge read charsets
        // other than UTF-8 because it's not supported by the JVM.
        given:
        def attributeNameMerged = 'Looong-Name-Of-Manifest-Entry'
        def attributeNameWritten = 'Another-Looooooong-Name-Entry'
        // Means 'long russian text'
        def attributeValue = 'com.acme.example.pack.**, ??????????????.??????????.????.??????????????.??????????.**'

        def mergedManifestFilename = 'manifest-with-split-multi-byte-char.txt'
        def mergedManifest = new Manifest()
        mergedManifest.mainAttributes.putValue('Manifest-Version', '1.0')
        mergedManifest.mainAttributes.putValue(attributeNameMerged, attributeValue)
        def mergedManifestFile = file(mergedManifestFilename)
        mergedManifestFile.withOutputStream { mergedManifest.write(it) }

        buildScript """
            $taskTypeDeclaration
            task jar(type: $taskType) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
                manifest {
                    attributes '$attributeNameWritten': '$attributeValue'
                    from file('$mergedManifestFilename')
                }
            }
        """.stripIndent()

        when:
        executer.withDefaultCharacterEncoding('windows-1252').withTasks('jar')
        executer.run()

        then:
        def jar = new JarFile(file('dest/test.jar'))
        try {
            def manifest = jar.manifest
            assert manifest.mainAttributes.getValue(attributeNameWritten) == attributeValue
            assert manifest.mainAttributes.getValue(attributeNameMerged) == attributeValue
        } finally {
            jar.close()
        }

        where:
        taskType            | taskTypeDeclaration
        'Jar'               | ''
        'CustomJarManifest' | customJarManifestTask()
    }

    @Issue('GRADLE-3374')
    @Unroll
    def "reports error for unsupported manifest content charsets, write #writeCharset, read #readCharset"() {
        given:
        settingsFile << "rootProject.name = 'root'"
        buildScript """
            task jar(type: Jar) {
                from file('test')
                destinationDir = file('dest')
                archiveName = 'test.jar'
                manifestContentCharset = $writeCharset
                manifest {
                    from('manifest-to-merge.txt') {
                        contentCharset = $readCharset
                    }
                }
            }
        """.stripIndent()

        when:
        executer.withDefaultCharacterEncoding('UTF-8')
        fails 'jar'

        then:
        failure.assertHasDescription("A problem occurred evaluating root project 'root'.")
        failure.assertHasCause(cause)

        where:
        writeCharset    | readCharset     | cause
        "'UNSUPPORTED'" | "'UTF-8'"       | "Charset for manifestContentCharset 'UNSUPPORTED' is not supported by your JVM"
        "'UTF-8'"       | "'UNSUPPORTED'" | "Charset for contentCharset 'UNSUPPORTED' is not supported by your JVM"
        null            | "'UTF-8'"       | "manifestContentCharset must not be null"
        "'UTF-8'"       | null            | "contentCharset must not be null"
    }

    def "JAR task is skipped when compiler output is unchanged"() {
        file("src/main/java/Main.java") << "public class Main {}\n"
        buildFile << """
            apply plugin: "java"
        """

        succeeds "jar"

        file("src/main/java/Main.java") << "// This should not influence compiled output"

        when:
        succeeds "jar"
        then:
        nonSkippedTasks.contains ":compileJava"
        skippedTasks.contains ":jar"
    }

    def "cannot create a JAR without destination dir"() {
        given:
        buildFile << """
            task jar(type: Jar) {
                archiveName = 'some.jar'
            }
        """

        when:
        fails('jar')

        then:
        failureCauseContains('No value has been specified for property \'archiveFile\'.')
    }

    private static String customJarManifestTask() {
        return '''
            class CustomJarManifest extends org.gradle.jvm.tasks.Jar {
                CustomJarManifest() {
                    super();
                    setManifest(new CustomManifest(getFileResolver()))
                }
            }

            class CustomManifest implements org.gradle.api.java.archives.Manifest {
                @Delegate org.gradle.api.java.archives.Manifest delegate

                CustomManifest(fileResolver) {
                    this.delegate = new org.gradle.api.java.archives.internal.DefaultManifest(fileResolver)
                }
            }
        '''.stripIndent()
    }
}
