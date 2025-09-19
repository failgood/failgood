import failgood.versions.Versions
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("failgood.versions")
    kotlin("jvm")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
    id("failgood.common")
    id("failgood.publishing")
    id("org.jetbrains.kotlinx.kover") version "0.9.2"
}

// Access versions object from the versions plugin
val versions: Versions by project.extra

// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype
// closeAndReleaseSonatypeStagingRepository)

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}")
    api("org.junit.platform:junit-platform-commons:${versions.junitPlatform}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${versions.coroutines}")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")

    // to enable running test in idea without having to add the dependency manually
    api("org.junit.platform:junit-platform-launcher:${versions.junitPlatform}")
    compileOnly("org.junit.platform:junit-platform-engine:${versions.junitPlatform}")

    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:${versions.pitest}")
    implementation("org.opentest4j:opentest4j:1.3.0")
    testImplementation("org.pitest:pitest:${versions.pitest}")
    testImplementation("org.junit.platform:junit-platform-engine:${versions.junitPlatform}")
    testImplementation("io.projectreactor.tools:blockhound:1.0.14.RELEASE")

    testImplementation(kotlin("test"))
    testImplementation("ch.qos.logback:logback-classic:1.5.18")

    // for the tools that analyze what events jupiter tests generate.
    testImplementation("org.junit.jupiter:junit-jupiter-api:${versions.junitJupiter}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${versions.junitJupiter}")
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${versions.coroutines}")
}

sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}

sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
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
        pitestVersion = versions.pitest
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}

// this seems to be no longer necessary, but keeping it here for now
// tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
