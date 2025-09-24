@file:Suppress("GradlePackageUpdate")

import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("shared.common")
    //    id("failgood.publishing")
    alias(libs.plugins.kover)
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("customTestPlugin") {
            id = "failgood.gradle.FailgoodPlugin"
            implementationClass = "failgood.gradle.FailgoodPlugin"
        }
    }
}

// Modify the test task to depend on the metadata generation
tasks.test {
    // Ensure the test task depends on pluginUnderTestMetadata
    dependsOn(tasks.pluginUnderTestMetadata)

    // Add the metadata to the test classpath
    doFirst { classpath += files(tasks.pluginUnderTestMetadata.get().outputDirectory) }
    useJUnitPlatform()

    // Don't fail when no tests are found (test is commented out)
    // this is necessary in gradle 9.0 and not supported in gradle 8.x so we keep it commented out
    // for now
    //    failOnNoDiscoveredTests = false
}

dependencies {
    testImplementation(libs.kotlin.test)
    implementation(project(":failgood"))
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit5)

    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.engine)
    implementation(libs.junit.platform.launcher)
    implementation(libs.junit.platform.engine)
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        addJUnitPlatformLauncher = false
        jvmArgs = listOf("-Xmx512m") // necessary on CI
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")
        targetClasses = setOf("failgood.*") // by default "${project.group}.*"
        targetTests = setOf("failgood.*Test", "failgood.**.*Test")
        pitestVersion = libs.versions.pitest.get()
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}

@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
