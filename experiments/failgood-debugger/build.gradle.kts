@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("failgood.common")
//    id("failgood.publishing")
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
    id("org.jetbrains.kotlinx.kover") version "0.7.4"
    id("org.jetbrains.dokka") version "1.9.10"
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":failgood"))
    // it seems tools.jar is currently not necessary to compile this. I'm pretty sure that  was necessary at some point
    // I'm keeping it here because this is an experiment anyway.
//    compileOnly(files("${System.getenv("java.home")}/../lib/tools.jar"))
}
sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}
sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("test-resources")
}
plugins.withId("info.solidsoft.pitest") {
    configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
//                verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        excludedTestClasses.set(setOf("failgood.MultiThreadingPerformanceTest*"))
        targetClasses.set(setOf("failgood.*")) // by default "${project.group}.*"
        targetTests.set(setOf("failgood.*Test", "failgood.**.*Test"))
        pitestVersion.set(failgood.versions.pitestVersion)
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}
configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
