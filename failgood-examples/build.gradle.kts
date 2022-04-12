@file:Suppress("GradlePackageUpdate")
import failgood.versions.coroutinesVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension

/**
 * A kotlin project that uses failgood as test runner and pitest for mutation coverage.
 */
plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.6.1")

    // everything else is optional, and only here because some tests show interactions with these libs
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("io.mockk:mockk:1.12.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")
    implementation("org.slf4j:slf4j-api:1.7.36")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}



plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        mutators.set(listOf("ALL"))
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failgood.examples.*")) // by default "${project.group}.*"
        targetTests.set(setOf("failgood.examples.*Test", "failgood.examples.**.*Test"))
        pitestVersion.set("1.6.7")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

// this is an example how to run the test via a main method.
// most projects will probably just use the junit platform engine via gradle
val testMain =
    task("testMain", JavaExec::class) {
        mainClass.set("failgood.examples.AllTestsKt")
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    mainClass.set("failgood.examples.AutoTestMainKt")
    classpath = sourceSets["test"].runtimeClasspath
}
tasks.check { dependsOn(testMain) }
