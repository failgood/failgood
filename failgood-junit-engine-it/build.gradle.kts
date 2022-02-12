@file:Suppress("GradlePackageUpdate") // disable until it works correctly

import failgood.versions.junitPlatformVersion
import failgood.versions.striktVersion

plugins {
    kotlin("jvm")
    id("com.bnorm.power.kotlin-power-assert").version("0.11.0")
//    id("info.solidsoft.pitest")
}

dependencies {
    testImplementation(project(":failgood"))
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.6.RELEASE")
    testImplementation(kotlin("test"))
}

val testMain =
    task("testMain", JavaExec::class) {
        jvmArgs = listOf("-XX:+AllowRedefinitionToAddDeleteMethods")
        mainClass.set("failgood.junit.it.AllTestsKt")
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    mainClass.set("failgood.junit.it.AutoTestMainKt")
    classpath = sourceSets["test"].runtimeClasspath
}
tasks.test {
    jvmArgs = listOf("-XX:+AllowRedefinitionToAddDeleteMethods")
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

tasks.check { dependsOn(testMain) }
/*
plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failgood")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failgood.junit.it.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failgood.junit.it.*Test", "failgood.examples.junit.it.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

*/
configure<com.bnorm.power.PowerAssertGradleExtension> {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
