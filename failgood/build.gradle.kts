import failgood.versions.coroutinesVersion
import failgood.versions.junitJupiterVersion
import failgood.versions.junitPlatformVersion
import failgood.versions.pitestVersion
import failgood.versions.striktVersion
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import failgood.versions.*
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("multiplatform")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
//    id("failgood.common")
//    id("failgood.publishing")
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("org.jetbrains.dokka") version "2.0.0"
}

// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype
// closeAndReleaseSonatypeStagingRepository)

/*
dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.4")

    // to enable running test in idea without having to add the dependency manually
    api("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:$pitestVersion")
    implementation("org.opentest4j:opentest4j:1.3.0")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.pitest:pitest:$pitestVersion")
    testImplementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
    testImplementation("io.projectreactor.tools:blockhound:1.0.11.RELEASE")

    testImplementation(kotlin("test"))
    testImplementation("ch.qos.logback:logback-classic:1.5.16")

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
*/
val enableJs = false
kotlin {
/* waiting for compatible libraries
    wasmWasi {
        nodejs()
        binaries.executable()
    }*/
    if (enableJs) {
        js {
            nodejs {}
        }
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
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
        val commonMain by getting {
            kotlin.srcDir("src@common")
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
        val commonTest by getting {
            kotlin.srcDir("test@common")
            dependencies {
                implementation(kotlin("test"))
            }
        }
        if (enableJs) {
            val jsMain by getting { kotlin.srcDir("src@js") }
            val jsTest by getting { kotlin.srcDir("test@js") }
        }

        val jvmMain by getting {
            kotlin.srcDir("src")
            resources.srcDir("resources")
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
//                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
                // to enable running test in idea without having to add the dependency manually
                api("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
                compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

                implementation(kotlin("stdlib-jdk8"))
                compileOnly("org.pitest:pitest:$pitestVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("test")
            resources.srcDir("testResources")
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
tasks {
    test {
        useJUnitPlatform {
// use all engine for now because we want to see the playground engines output
            //            includeEngines = setOf("failgood")
        }
        outputs.upToDateWhen { false }
    }

    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            if (System.getenv("CI") != null)
                allWarningsAsErrors = true
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            languageVersion = "1.6"
            apiVersion = "1.6"
        }
    }
}
configure<TestLoggerExtension> {
    theme = ThemeType.MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}
tasks.getByName("check").dependsOn(tasks.getByName("ktfmtCheck"))

ktfmt {
    kotlinLangStyle()
}
