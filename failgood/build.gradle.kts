import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    kotlin("jvm")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
    id("shared.common")
    id("shared.publishing")
    alias(libs.plugins.kover)
}

// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype
// closeAndReleaseSonatypeStagingRepository)

dependencies {
    compileOnly(libs.kotlinx.coroutines.core)
    api(libs.junit.platform.commons)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.logging)

    api(libs.junit.platform.launcher)
    compileOnly(libs.junit.platform.engine)

    implementation(libs.kotlin.stdlib.jdk8)
    compileOnly(libs.pitest)
    implementation(libs.opentest4j)
    testImplementation(libs.pitest)
    testImplementation(libs.junit.platform.engine)
    testImplementation(libs.blockhound)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.logback.classic)

    // for the tools that analyze what events jupiter tests generate.
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.kotlinx.coroutines.debug)
}

tasks {
    val testMain =
        register("testMain", JavaExec::class) {
            mainClass = "failgood.FailGoodBootstrapKt"
            classpath = sourceSets["test"].runtimeClasspath
        }
    val multiThreadedTest =
        register("multiThreadedTest", JavaExec::class) {
            mainClass = "failgood.MultiThreadingPerformanceTestKt"
            classpath = sourceSets["test"].runtimeClasspath
            systemProperties = mapOf("kotlinx.coroutines.scheduler.core.pool.size" to "1000")
        }

    register("autotest", JavaExec::class) {
        mainClass = "failgood.AutoTestMainKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
    check { dependsOn(testMain, multiThreadedTest) }

    // reproduce https://github.com/failgood/failgood/issues/93
    register<Test>("runSingleNonFailgoodTest") {
        outputs.upToDateWhen { false }
        include("**/NonFailgoodTest.class")
        useJUnitPlatform()
    }
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        // in case of problems:
        //                verbose = true
        verbose = false
        addJUnitPlatformLauncher = false
        jvmArgs =
            listOf(
                "-Xmx512m", // necessary on CI
                "-Djava.util.logging.config.file=${rootProject.projectDir}/pitest.logging.properties")
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")
        excludedTestClasses = setOf("failgood.MultiThreadingPerformanceTest*")
        targetClasses = setOf("failgood.*") // by default "${project.group}.*"
        targetTests = setOf("failgood.*Test", "failgood.**.*Test")
        pitestVersion = libs.versions.pitest.get()
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}

// this seems to be no longer necessary, but keeping it here for now
// tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
