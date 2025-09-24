@file:Suppress("GradlePackageUpdate")

import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("shared.common")
    //    id("failgood.publishing")
    alias(libs.plugins.kover)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":failgood"))
    // it seems tools.jar is currently not necessary to compile this. I'm pretty sure that was
    // necessary at some point
    // I'm keeping it here because this is an experiment anyway.
    //    compileOnly(files("${System.getenv("java.home")}/../lib/tools.jar"))
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
