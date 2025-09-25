import buildgood.CommonBuildExtension
import buildgood.PublishingBuildExtension

// This plugin configures the project-specific settings for failgood
// Apply this in your root build.gradle.kts to configure all modules

// Read project-specific configuration from gradle.properties or set defaults
val projectName = findProperty("project.name") as String? ?: "FailGood"
val projectDescription = findProperty("project.description") as String? ?: "a fast test runner for kotlin"
val projectUrl = findProperty("project.url") as String? ?: "https://github.com/failgood/failgood"
val projectRepo = findProperty("project.repo") as String? ?: "failgood/failgood"
val jvmProduction = findProperty("jvm.production") as String? ?: "1.8"
val jvmTest = findProperty("jvm.test") as String? ?: "17"
val useFailgoodAsserts = findProperty("use.failgood.asserts")?.toString()?.toBoolean() ?: true
val useStrictMode = findProperty("use.strict.mode")?.toString()?.toBoolean() ?: true

// Apply configuration to common plugin
pluginManager.withPlugin("shared.common") {
    configure<CommonBuildExtension> {
        jvmTarget {
            production(jvmProduction.replace("1.", "").toIntOrNull() ?: 8)
            test(jvmTest.replace("1.", "").toIntOrNull() ?: 17)
        }
        if (useFailgoodAsserts) {
            useFailgoodPowerAssert()
        } else {
            useBasicPowerAssert()
        }
        if (useStrictMode) {
            useStrictKotlinMode()
        }
    }
}

// Apply configuration to publishing plugin
pluginManager.withPlugin("shared.publishing") {
    configure<PublishingBuildExtension> {
        projectInfo {
            name = projectName
            description = projectDescription
            url = projectUrl
        }
        scm {
            fromGitHub(projectRepo)
        }
    }
}