plugins {
    id("buildgood.kmp")
    id("buildgood.pitest")
    `maven-publish`
    signing
    alias(libs.plugins.kover)
}

// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or ./gradlew publishToSonatype
// closeAndReleaseSonatypeStagingRepository)

publish {}

kotlin {
    jvm {
        compilations.getByName("test") {
            val testMain =
                tasks.register("testMain", JavaExec::class) {
                    enableAssertions = true
                    mainClass.set("failgood.FailGoodBootstrapKt")
                    classpath(runtimeDependencyFiles, output)
                }
            val multiThreadedTest =
                tasks.register("multiThreadedTest", JavaExec::class) {
                    enableAssertions = true
                    mainClass.set("failgood.MultiThreadingPerformanceTestKt")
                    classpath(runtimeDependencyFiles, output)
                    systemProperties =
                        mapOf("kotlinx.coroutines.scheduler.core.pool.size" to "1000")
                }

            tasks.register("autotest", JavaExec::class) {
                enableAssertions = true
                mainClass.set("failgood.AutoTestMainKt")
                classpath(runtimeDependencyFiles, output)
            }

            tasks.register("runSingleNonFailgoodTest", Test::class) {
                outputs.upToDateWhen { false }
                testClassesDirs = output.classesDirs
                classpath = files(runtimeDependencyFiles, output)
                include("**/NonFailgoodTest.class")
                failOnNoDiscoveredTests = false
                useJUnitPlatform()
            }

            tasks.register("test", Test::class) {
                description = "Runs the JVM test suite with the standard Gradle test entry point"
                group = "verification"
                testClassesDirs = output.classesDirs
                classpath = files(runtimeDependencyFiles, output)
                useJUnitPlatform()
            }

            tasks.named("check") { dependsOn(testMain, multiThreadedTest) }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting { dependencies { implementation(libs.kotlinx.coroutines.core) } }
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val jvmMain by getting {
            dependencies {
                api(libs.junit.platform.commons)
                api(libs.junit.platform.launcher)
                compileOnly(libs.junit.platform.engine)
                compileOnly(libs.pitest)
                implementation(libs.kotlin.stdlib.jdk8)
                implementation(libs.kotlin.logging)
                implementation(libs.kotlinx.coroutines.slf4j)
                implementation(libs.opentest4j)
                implementation(libs.slf4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.christophsturm:filepeek:0.1.3") // IDEA misses it transitively
                implementation(kotlin("test"))
                implementation(libs.blockhound)
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.engine)
                implementation(libs.junit.platform.engine)
                implementation(libs.junit.platform.launcher)
                implementation(libs.logback.classic)
                implementation(libs.pitest)
                runtimeOnly(libs.kotlinx.coroutines.debug)
            }
        }
    }
}

// this seems to be no longer necessary, but keeping it here for now
// tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
