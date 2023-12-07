@file:Suppress("GradlePackageUpdate")

import info.solidsoft.gradle.pitest.PitestPluginExtension


plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("failgood.common")
//    id("failgood.publishing")
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
    id("org.jetbrains.kotlinx.kover") version "0.7.5"
    id("org.jetbrains.dokka") version "1.9.10"
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":failgood"))
    // it seems tools.jar is currently not necessary to compile this. I'm pretty sure that was necessary at some point
    // I'm keeping it here because this is an experiment anyway.
//    compileOnly(files("${System.getenv("java.home")}/../lib/tools.jar"))
}
sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}
sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}
plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        addJUnitPlatformLauncher = false
        jvmArgs = listOf("-Xmx512m") // necessary on CI
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")
        targetClasses = setOf("failgood.*") // by default "${project.group}.*"
        targetTests = setOf("failgood.*Test", "failgood.**.*Test")
        pitestVersion = failgood.versions.pitestVersion
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}
configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
