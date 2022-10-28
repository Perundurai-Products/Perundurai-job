package configurations_g4

import jetbrains.buildServer.configs.kotlin.v2018_1.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2018_1.BuildSteps
import jetbrains.buildServer.configs.kotlin.v2018_1.buildSteps.GradleBuildStep
import model_g4.CIBuildModel
import model_g4.Stage

class Gradleception(model: CIBuildModel, stage: Stage) : BaseGradleBuildType(model, stage = stage, init = {
    uuid = "${model.projectPrefix}Gradleception"
    id = AbsoluteId(uuid)
    name = "Gradleception - Java8 Linux"
    description = "Builds Gradle with the version of Gradle which is currently under development (twice)"

    params {
        param("env.JAVA_HOME", buildJavaHome)
    }

    val buildScanTagForType = buildScanTag("Gradleception")
    val defaultParameters = (gradleParameters() + listOf(buildScanTagForType)).joinToString(separator = " ")

    applyDefaults(model, this, ":install", notQuick = true, extraParameters = "-Pgradle_installPath=dogfood-first $buildScanTagForType", extraSteps = {
        localGradle {
            name = "BUILD_WITH_BUILT_GRADLE"
            tasks = "clean :install"
            gradleHome = "%teamcity.build.checkoutDir%/dogfood-first"
            gradleParams = "-Pgradle_installPath=dogfood-second $defaultParameters"
        }
        localGradle {
            name = "QUICKCHECK_WITH_GRADLE_BUILT_BY_GRADLE"
            tasks = "clean sanityCheck test"
            gradleHome = "%teamcity.build.checkoutDir%/dogfood-second"
            gradleParams = defaultParameters
        }
    })
})

fun BuildSteps.localGradle(init: GradleBuildStep.() -> Unit): GradleBuildStep =
    customGradle(init) {
        param("ui.gradleRunner.gradle.wrapper.useWrapper", "false")
        buildFile = ""
    }
