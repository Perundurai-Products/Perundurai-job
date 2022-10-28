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

package org.gradle.api.plugins.quality.jdepend

import org.gradle.integtests.fixtures.AbstractProjectRelocationIntegrationTest
import org.gradle.test.fixtures.file.TestFile

class JDependRelocationIntegrationTest extends AbstractProjectRelocationIntegrationTest {

    @Override
    protected String getTaskName() {
        return ":jdepend"
    }

    @Override
    protected void setupProjectIn(TestFile projectDir) {
        projectDir.file("src/main/java/org/gradle/Class1.java") <<
            "package org.gradle; class Class1 { public boolean is() { return true; } }"
        projectDir.file("src/main/java/org/gradle/Class1Test.java") <<
            "package org.gradle; class Class1Test { public boolean is() { return true; } }"

        projectDir.file("build.gradle") << """
            apply plugin: "jdepend"

            ${mavenCentralRepository()}

            task compile(type: JavaCompile) {
                sourceCompatibility = JavaVersion.current()
                targetCompatibility = JavaVersion.current()
                destinationDir = file("build/classes")
                source "src/main/java"
                classpath = files()
            }

            task jdepend(type: JDepend) {
                dependsOn compile
                classesDirs = files("build/classes")
            }
        """
    }

    @Override
    protected extractResultsFrom(TestFile projectDir) {
        projectDir.file("build/reports/jdepend/jdepend.xml").text
    }
}
