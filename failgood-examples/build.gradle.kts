@file:Suppress("GradlePackageUpdate")

import com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy
import info.solidsoft.gradle.pitest.PitestPluginExtension

/** A kotlin project that uses failgood as test runner and pitest for mutation coverage.
 * this build does not use the common build logic because it is an example meant to work standalone */
plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("com.ncorti.ktfmt.gradle") version "0.24.0"
    kotlin("plugin.power-assert") version "2.1.21"
}

dependencies {
    testImplementation(project(":failgood"))

    // everything else is optional, and only here because some tests show interactions with these
    // libs
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging.microutils)
    implementation(libs.slf4j.api)
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        addJUnitPlatformLauncher = false
        mutators.set(listOf("ALL"))
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failgood.examples.*")) // by default "${project.group}.*"
        targetTests.set(setOf("failgood.examples.*Test", "failgood.examples.**.*Test"))
        pitestVersion.set(libs.versions.pitest.get())
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors())
        outputFormats.set(setOf("XML", "HTML"))
    }
}

tasks {
    withType<Test> { useJUnitPlatform() }

    // this is an example how to run the test via a main method.
    // most projects will probably just use the junit platform engine via gradle
    val testMain =
        register("testMain", JavaExec::class) {
            mainClass.set("failgood.examples.AllTestsKt")
            classpath = sourceSets["test"].runtimeClasspath
        }

    register("autotest", JavaExec::class) {
        mainClass.set("failgood.examples.AutoTestMainKt")
        classpath = sourceSets["test"].runtimeClasspath
    }
    getByName("check").dependsOn(getByName("ktfmtCheck"))
    check { dependsOn(testMain) }
}

sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}

sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}

ktfmt {
    kotlinLangStyle()
    trailingCommaManagementStrategy = TrailingCommaManagementStrategy.NONE
}
