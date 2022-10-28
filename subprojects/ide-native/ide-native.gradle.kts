import org.gradle.gradlebuild.unittestandcompile.ModuleType

/*
 * Copyright 2014 the original author or authors.
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
plugins {
    gradlebuild.classycle
}

dependencies {
    compile(library("groovy"))
    compile(project(":core"))
    compile(project(":ide"))
    compile(project(":platformNative"))
    compile(project(":languageNative"))
    compile(project(":testingNative"))
    compile(library("plist"))

    testFixturesApi(project(":internalTesting"))
}

gradlebuildJava {
    moduleType = ModuleType.CORE
}

testFixtures {
    from(":core")
    from(":platformNative")
    from(":languageNative")
    from(":versionControl")
    from(":ide", "testFixtures")
}
