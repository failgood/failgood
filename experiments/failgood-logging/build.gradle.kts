@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("failgood.common")
//    id("failgood.publishing")
    kotlin("plugin.power-assert") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.0"
    id("org.jetbrains.dokka") version "1.9.20"
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":failgood"))
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.slf4j:slf4j-api:2.0.7")
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
@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertNotNull",
        "failgood.softly.AssertDSL.assert"
    )
}
