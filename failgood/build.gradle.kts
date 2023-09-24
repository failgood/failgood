@file:Suppress("GradlePackageUpdate")

import com.adarshr.gradle.testlogger.TestLoggerExtension
import failgood.versions.coroutinesVersion
import failgood.versions.junitPlatformVersion
import failgood.versions.pitestVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
//    id("failgood.common")
//    id("failgood.publishing")
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
    id("org.jetbrains.kotlinx.kover") version "0.7.3"
    id("org.jetbrains.dokka") version "1.8.20"
    id("org.jmailen.kotlinter")
    id("com.adarshr.test-logger")
}
// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository)

dependencies {
/*    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
    // to enable running test in idea without having to add the dependency manually
    api("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:$pitestVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.opentest4j:opentest4j:1.3.0")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.pitest:pitest:$pitestVersion")
    testImplementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.8.RELEASE")

    testImplementation(kotlin("test"))

    // for the tools that analyze what events jupiter tests generate.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")

 */
}
kotlin {
    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform {}
        }
        compilations.getByName("test") {
            val testMain =
                task("testMain", JavaExec::class) {
                    mainClass.set("failgood.FailGoodBootstrapKt")
                    classpath(runtimeDependencyFiles, output)
                }
            val multiThreadedTest =
                task("multiThreadedTest", JavaExec::class) {
                    mainClass.set("failgood.MultiThreadingPerformanceTestKt")
                    classpath(runtimeDependencyFiles, output)
                    systemProperties = mapOf("kotlinx.coroutines.scheduler.core.pool.size" to "1000")
                }
            task("autotest", JavaExec::class) {
                mainClass.set("failgood.AutoTestMainKt")
                classpath(runtimeDependencyFiles, output)
            }

            tasks.check { dependsOn(testMain, multiThreadedTest) }

        }
    }
    sourceSets {
        val commonMain by getting { kotlin.srcDir("common/src") }
        val commonTest by getting { kotlin.srcDir("common/test") }
        val jvmMain by getting {
            kotlin.srcDir("jvm/src")
            resources.srcDir("jvm/resources")
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
                // to enable running test in idea without having to add the dependency manually
                api("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
                compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

                implementation(kotlin("stdlib-jdk8"))
                compileOnly("org.pitest:pitest:$pitestVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("jvm/test")
            resources.srcDir("jvm/test-resources")
            dependencies {
                implementation("io.strikt:strikt-core:$striktVersion")
                implementation("org.pitest:pitest:$pitestVersion")
                implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
                implementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
                implementation("io.projectreactor.tools:blockhound:1.0.6.RELEASE")
                implementation(kotlin("test"))

            }
        }
    }
}
//tasks.check { dependsOn(testMain, multiThreadedTest) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
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
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertNotNull",
        "failgood.softly.AssertDSL.assert"
    )
}

// reproduce https://github.com/failgood/failgood/issues/93
tasks.register<Test>("runSingleNonFailgoodTest") {
    outputs.upToDateWhen { false }
    include("**/NonFailgoodTest.class")
    useJUnitPlatform()
}

tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}
