@file:Suppress("GradlePackageUpdate")

import failgood.versions.coroutinesVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension

/** A kotlin project that uses failgood as test runner and pitest for mutation coverage. */
plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("com.ncorti.ktfmt.gradle")
    kotlin("plugin.power-assert")
}

dependencies {
    testImplementation(project(":failgood"))

    // everything else is optional, and only here because some tests show interactions with these
    // libs
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("io.mockk:mockk:1.14.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("org.slf4j:slf4j-api:2.0.17")
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
        pitestVersion.set("1.19.5")
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
    manageTrailingCommas = false
}
