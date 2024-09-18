import failgood.versions.coroutinesVersion
import failgood.versions.junitJupiterVersion
import failgood.versions.junitPlatformVersion
import failgood.versions.kotlinVersion
import failgood.versions.pitestVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    kotlin("jvm")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
    id("failgood.common")
    id("failgood.publishing")
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.dokka") version "1.9.20"
}
// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository)

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")

    // to enable running test in idea without having to add the dependency manually
    api("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:$pitestVersion")
    implementation("org.opentest4j:opentest4j:1.3.0")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.pitest:pitest:$pitestVersion")
    testImplementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.9.RELEASE")

    testImplementation(kotlin("test"))
    testImplementation("ch.qos.logback:logback-classic:1.5.8")


    // for the tools that analyze what events jupiter tests generate.
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
}
sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}
sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}
val testMain =
    task("testMain", JavaExec::class) {
        mainClass = "failgood.FailGoodBootstrapKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
val multiThreadedTest =
    task("multiThreadedTest", JavaExec::class) {
        mainClass = "failgood.MultiThreadingPerformanceTestKt"
        classpath = sourceSets["test"].runtimeClasspath
        systemProperties = mapOf("kotlinx.coroutines.scheduler.core.pool.size" to "1000")
    }
task("autotest", JavaExec::class) {
    mainClass = "failgood.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check { dependsOn(testMain, multiThreadedTest) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
// in case of problems:
        //                verbose = true
        addJUnitPlatformLauncher = false
        jvmArgs = listOf("-Xmx512m") // necessary on CI
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")
        excludedTestClasses = setOf("failgood.MultiThreadingPerformanceTest*")
        targetClasses = setOf("failgood.*") // by default "${project.group}.*"
        targetTests = setOf("failgood.*Test", "failgood.**.*Test")
        pitestVersion = failgood.versions.pitestVersion
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}


// reproduce https://github.com/failgood/failgood/issues/93
tasks.register<Test>("runSingleNonFailgoodTest") {
    outputs.upToDateWhen { false }
    include("**/NonFailgoodTest.class")
    useJUnitPlatform()
}

// this seems to be no longer necessary, but keeping it here for now
// tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
