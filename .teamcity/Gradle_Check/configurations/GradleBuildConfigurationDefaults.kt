package configurations

import jetbrains.buildServer.configs.kotlin.v2018_1.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildFeatures
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildStep
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_1.CheckoutMode
import jetbrains.buildServer.configs.kotlin.v2018_1.FailureAction
import jetbrains.buildServer.configs.kotlin.v2018_1.ProjectFeatures
import jetbrains.buildServer.configs.kotlin.v2018_1.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.GradleBuildStep
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.script
import model.CIBuildModel
import model.GradleSubproject
import model.OS
import model.TestCoverage

private val java7Homes = mapOf(
        OS.windows to """"-Djava7Home=%windows.java7.oracle.64bit%"""",
        OS.linux to "-Djava7Home=%linux.jdk.for.gradle.compile%",
        OS.macos to "-Djava7Home=%macos.java7.oracle.64bit%"
)

private val java9Homes = mapOf(
    OS.windows to """"-Djava9Home=%windows.java9.oracle.64bit%"""",
    OS.linux to "-Djava9Home=%linux.java9.oracle.64bit%",
    OS.macos to "-Djava9Home=%macos.java9.oracle.64bit%"
)


fun shouldBeSkipped(subProject: GradleSubproject, testConfig: TestCoverage): Boolean {
    // TODO: Hacky. We should really be running all the subprojects on macOS
    // But we're restricting this to just a subset of projects for now
    // since we only have a small pool of macOS agents
    return testConfig.os.ignoredSubprojects.contains(subProject.name)
}

fun gradleParameters(os: OS = OS.linux, daemon: Boolean = true): List<String> =
    listOf(
        "-PmaxParallelForks=%maxParallelForks%",
        "-s",
        if (daemon) "--daemon" else "--no-daemon",
        "--continue",
        """-I "%teamcity.build.checkoutDir%/gradle/init-scripts/build-scan.init.gradle.kts"""",
        java7Homes[os]!!,
        java9Homes[os]!!,
        "-Dorg.gradle.internal.tasks.createops")


val m2CleanScriptUnixLike = """
    REPO=%teamcity.agent.jvm.user.home%/.m2/repository
    if [ -e ${'$'}REPO ] ; then
        tree ${'$'}REPO
        rm -rf ${'$'}REPO
        echo "${'$'}REPO was polluted during the build"
        return 1
    else
        echo "${'$'}REPO does not exist"
    fi
""".trimIndent()

val m2CleanScriptWindows = """
    IF exist %teamcity.agent.jvm.user.home%\.m2\repository (
        TREE %teamcity.agent.jvm.user.home%\.m2\repository
        RMDIR /S /Q %teamcity.agent.jvm.user.home%\.m2\repository
        EXIT 1
    )
""".trimIndent()

fun applyDefaultSettings(buildType: BuildType, os: OS = OS.linux, timeout: Int = 30, vcsRoot: String = "Gradle_Branches_GradlePersonalBranches") {
    buildType.artifactRules = """
        build/report-* => .
        buildSrc/build/report-* => .
        subprojects/*/build/tmp/test files/** => test-files
        build/errorLogs/** => errorLogs
    """.trimIndent()

    buildType.vcs {
        root(AbsoluteId(vcsRoot))
        checkoutMode = CheckoutMode.ON_AGENT
        buildDefaultBranch = !vcsRoot.contains("Branches")
    }

    buildType.requirements {
        contains("teamcity.agent.jvm.os.name", os.agentRequirement)
    }

    buildType.failureConditions {
        executionTimeoutMin = timeout
    }

    if (os == OS.linux || os == OS.macos) {
        buildType.params {
            param("env.LC_ALL", "en_US.UTF-8")
        }
    }

    param("env.CI_REQUIRES_INVESTIGATION", "true")
    param("env.GRADLE_OPTS", "-XX:MaxPermSize=512m")
    param("env.REPO_MIRROR_URLS", "jcenter:http://dev12.gradle.org:8081/artifactory/jcenter," +
        "mavencentral:http://dev12.gradle.org:8081/artifactory/repo1," +
        "typesafemaven:http://dev12.gradle.org:8081/artifactory/typesafe-maven-releases," +
        "typesafeivy:http://dev12.gradle.org:8081/artifactory/typesafe-ivy-releases," +
        "google:http://dev12.gradle.org:8081/artifactory/google," +
        "lightbendmaven:http://dev12.gradle.org:8081/artifactory/typesafe-maven-releases," +
        "lightbendivy:http://dev12.gradle.org:8081/artifactory/typesafe-ivy-releases," +
        "springreleases:http://dev12.gradle.org:8081/artifactory/spring-releases/," +
        "springsnapshots:http://dev12.gradle.org:8081/artifactory/spring-snapshots/," +
        "restlet:http://dev12.gradle.org:8081/artifactory/restlet/," +
        "gradle-snapshots:http://dev12.gradle.org:8081/artifactory/gradle-snapshots/," +
        "gradle-releases:http://dev12.gradle.org:8081/artifactory/gradle-releases/," +
        "gradle:http://dev12.gradle.org:8081/artifactory/gradle-repo/," +
        "jboss:http://dev12.gradle.org:8081/artifactory/jboss/," +
        "gradleplugins:http://dev12.gradle.org:8081/artifactory/gradle-plugins/," +
        "gradlejavascript:http://dev12.gradle.org:8081/artifactory/gradle-javascript/," +
        "kotlindev:http://dev12.gradle.org:8081/artifactory/kotlin-dev/," +
        "kotlineap:http://dev12.gradle.org:8081/artifactory/kotlin-eap/")
}

fun BuildFeatures.publishBuildStatusToGithub() {
    commitStatusPublisher {
        vcsRootExtId = "Gradle_Branches_GradlePersonalBranches"
        publisher = github {
            githubUrl = "https://api.github.com"
            authType = personalToken {
                token = "credentialsJSON:5306bfc7-041e-46e8-8d61-1d49424e7b04"
            }
        }
    }
}

fun ProjectFeatures.buildReportTab(title: String, startPage: String) {
    feature {
        type = "ReportTab"
        param("startPage", startPage)
        param("title", title)
        param("type", "BuildReportTab")
    }
}

fun applyDefaults(model: CIBuildModel, buildType: BaseGradleBuildType, gradleTasks: String, notQuick: Boolean = false, os: OS = OS.linux, extraParameters: String = "", timeout: Int = 90, extraSteps: BuildSteps.() -> Unit = {}, daemon: Boolean = true) {
    applyDefaultSettings(buildType, os, timeout)

    var gradleParameterString = gradleParameters(os, daemon).joinToString(separator = " ")

    val buildScanTags = model.buildScanTags + listOfNotNull(buildType.stage?.id)

    buildType.steps {
        gradleWrapper {
            name = "CLEAN_BUILD_SRC"
            tasks = "clean"
            gradleParams = gradleParameterString
            workingDir = "buildSrc"
            gradleWrapperPath = ".."
            buildFile = "build.gradle.kts"
        }
        gradleWrapper {
            name = "GRADLE_RUNNER"
            tasks = "clean $gradleTasks"
            gradleParams = (
                    listOf(gradleParameterString) +
                            buildType.buildCache.gradleParameters(os) +
                            listOf(extraParameters) +
                            "-PteamCityUsername=%teamcity.username.restbot%" +
                            "-PteamCityPassword=%teamcity.password.restbot%" +
                            "-PteamCityBuildId=%teamcity.build.id%" +
                            buildScanTags.map { buildScanTag(it) }
                    ).joinToString(separator = " ")
        }
    }

    buildType.steps.extraSteps()

    buildType.steps {
        script {
            name = "CHECK_CLEAN_M2"
            executionMode = BuildStep.ExecutionMode.ALWAYS
            scriptContent = if (os == OS.windows) m2CleanScriptWindows else m2CleanScriptUnixLike
        }
        gradleWrapper {
            name = "VERIFY_TEST_FILES_CLEANUP"
            tasks = "verifyTestFilesCleanup"
            gradleParams = gradleParameterString
        }
        if (os == OS.windows) {
            gradleWrapper {
                name = "KILL_PROCESSES_STARTED_BY_GRADLE"
                executionMode = BuildStep.ExecutionMode.ALWAYS
                tasks = "killExistingProcessesStartedByGradle"
                gradleParams = gradleParameterString
            }
        }
        if (model.tagBuilds) {
            gradleWrapper {
                name = "TAG_BUILD"
                executionMode = BuildStep.ExecutionMode.ALWAYS
                tasks = "tagBuild"
                gradleParams = "$gradleParameterString -PteamCityUsername=%teamcity.username.restbot% -PteamCityPassword=%teamcity.password.restbot% -PteamCityBuildId=%teamcity.build.id% -PgithubToken=%github.ci.oauth.token%"
                buildFile = "gradle/buildTagging.gradle"
            }
        }
    }

    applyDefaultDependencies(model, buildType, notQuick)
}

fun buildScanTag(tag: String) = """"-Dscan.tag.$tag""""
fun buildScanCustomValue(key: String, value: String) = """"-Dscan.value.$key=$value""""

fun applyDefaultDependencies(model: CIBuildModel, buildType: BuildType, notQuick: Boolean = false) {
    if (notQuick) {
        // wait for quick feedback phase to finish successfully
        buildType.dependencies {
            dependency(AbsoluteId("${model.projectPrefix}Stage_QuickFeedback_Trigger")) {
                snapshot {
                    onDependencyFailure = FailureAction.CANCEL
                    onDependencyCancel = FailureAction.CANCEL
                }
            }

        }
    }

    if (buildType !is SanityCheck) {
        buildType.dependencies {
            val sanityCheckId = SanityCheck.buildTypeId(model)
            // Sanity Check has to succeed before anything else is started
            dependency(AbsoluteId(sanityCheckId)) {
                snapshot {
                    onDependencyFailure = FailureAction.CANCEL
                    onDependencyCancel = FailureAction.CANCEL
                }
            }
            // Get the build receipt from sanity check to reuse the timestamp
            artifacts(AbsoluteId(sanityCheckId)) {
                id = "ARTIFACT_DEPENDENCY_$sanityCheckId"
                cleanDestination = true
                artifactRules = "build-receipt.properties => incoming-distributions"
            }
        }
    }
}

/**
 * Adds a [Gradle build step](https://confluence.jetbrains.com/display/TCDL/Gradle)
 * that runs with the Gradle wrapper.
 *
 * @see GradleBuildStep
 */
fun BuildSteps.gradleWrapper(init: GradleBuildStep.() -> Unit): GradleBuildStep =
        customGradle(init) {
            useGradleWrapper = true
            if (buildFile == null) {
                buildFile = "" // Let Gradle detect the build script
            }
        }

fun BuildSteps.customGradle(init: GradleBuildStep.() -> Unit, custom: GradleBuildStep.() -> Unit): GradleBuildStep =
        GradleBuildStep(init)
                .apply(custom)
                .also { step(it) }
