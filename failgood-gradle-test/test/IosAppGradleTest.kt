package failgood.gradle

import failgood.Ignored
import failgood.Test
import failgood.testCollection
import java.io.File
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

private const val FAILGOOD_VERSION = "0.9.2"
private const val ROUNDTRIP_MARKER = "FAILGOOD_IOS_ROUNDTRIP_OK"

private val requiresMacOs = Ignored { if (isMacOs()) null else "requires macOS" }

@Test
class IosAppGradleTest {
    val tests =
        testCollection("ios app via gradle", ignored = requiresMacOs) {
            it("runs a failgood iosTest suite from an external KMP project") {
                publishFailgoodToMavenLocal()
                val projectDir = createConsumerProject()
                val result =
                    GradleRunner.create()
                        .withProjectDir(projectDir)
                        .withArguments(":app:iosAppTest", "--stacktrace")
                        .build()

                assert(result.output.contains("BUILD SUCCESSFUL"))
                assert(result.output.contains(ROUNDTRIP_MARKER))
                assert(result.task(":app:iosAppTest")?.outcome == TaskOutcome.SUCCESS)
                assert(result.task(":app:iosSimulatorArm64AppTest")?.outcome == TaskOutcome.SUCCESS)
                assert(result.task(":app:iosArm64AppTest") == null)
            }
        }
}

private fun createConsumerProject(): File {
    val projectDir = Files.createTempDirectory(buildDir().toPath(), "failgood-ios-gradle-").toFile()

    File(projectDir, "settings.gradle.kts")
        .writeText(
            """
            pluginManagement {
                includeBuild("../../../build-logic")
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    mavenLocal()
                    mavenCentral()
                }
            }

            rootProject.name = "failgood-ios-roundtrip"
            include(":app")
            """
                .trimIndent())
    File(projectDir, "gradle.properties")
        .writeText(
            """
            failgood.ios.testDevice=missing-device
            """
                .trimIndent())

    val appDir = File(projectDir, "app").apply { mkdirs() }
    File(appDir, "build.gradle.kts")
        .writeText(
            """
            plugins { id("buildgood.kmp") }

            repositories {
                mavenLocal()
                mavenCentral()
            }

            kotlin {
                iosArm64()
                iosSimulatorArm64()

                sourceSets {
                    val commonTest by getting {
                        dependencies {
                            implementation(kotlin("test"))
                            implementation("dev.failgood:failgood:$FAILGOOD_VERSION")
                        }
                    }
                }
            }
            """
                .trimIndent())
    File(appDir, "test@ios/sample/app/IosAppRoundtripTest.kt").apply {
        parentFile.mkdirs()
        writeText(
            """
            package sample.app

            import failgood.FailgoodIosBootstrap
            import failgood.testCollection
            import kotlin.test.Test
            import kotlin.test.assertTrue

            private object SmokeState {
                var didRun: Boolean = false
            }

            class IosAppRoundtripTest {
                @Test
                fun runsFailgoodViaGradle() {
                    SmokeState.didRun = false
                    val result =
                        FailgoodIosBootstrap(
                            "ios roundtrip",
                            testCollection("ios roundtrip", isolation = false) {
                                it("runs a passing failgood suite on iOS") {
                                    SmokeState.didRun = true
                                }
                                it("proves a failgood test body really ran") {
                                    assertTrue(SmokeState.didRun)
                                }
                            },
                        )
                            .run()

                    assertTrue(result.allOk)
                    println("$ROUNDTRIP_MARKER")
                }
            }
            """
                .trimIndent())
    }
    return projectDir
}

private fun buildDir(): File =
    File(IosAppGradleTest::class.java.protectionDomain.codeSource.location.toURI())
        .parentFile
        .parentFile
        .parentFile

private fun rootDir(): File = buildDir().parentFile.parentFile

private fun isMacOs(): Boolean = System.getProperty("os.name").contains("Mac", ignoreCase = true)

private fun publishFailgoodToMavenLocal() {
    val sourceCopyDir =
        Files.createTempDirectory(buildDir().toPath(), "failgood-publish-source-").toFile()
    copyPublishSource(rootDir(), sourceCopyDir)
    GradleRunner.create()
        .withProjectDir(sourceCopyDir)
        .withArguments(":failgood:publishToMavenLocal", "--stacktrace")
        .build()
}

private fun copyPublishSource(rootDir: File, targetDir: File) {
    copyProjectPath(File(rootDir, "build.gradle.kts"), File(targetDir, "build.gradle.kts"))
    copyProjectPath(File(rootDir, "gradle.properties"), File(targetDir, "gradle.properties"))
    copyProjectPath(
        File(rootDir, "pitest.logging.properties"),
        File(targetDir, "pitest.logging.properties"),
    )
    copyProjectPath(File(rootDir, "gradle"), File(targetDir, "gradle"))
    copyProjectPath(File(rootDir, "build-logic"), File(targetDir, "build-logic"))
    copyProjectPath(File(rootDir, "failgood"), File(targetDir, "failgood"))
    File(targetDir, "settings.gradle.kts")
        .writeText(
            """
            @file:Suppress("UnstableApiUsage")

            pluginManagement {
                repositories {
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }

            rootProject.name = "failgood-publish-source"
            includeBuild("build-logic")
            include(":failgood")
            """
                .trimIndent())
}

private fun copyProjectPath(source: File, target: File) {
    if (!source.exists()) return
    if (source.isDirectory) {
        if (source.name in setOf("build", ".gradle", ".idea", ".kotlin")) return
        target.mkdirs()
        source.listFiles()!!.forEach { child -> copyProjectPath(child, File(target, child.name)) }
        return
    }
    target.parentFile.mkdirs()
    source.copyTo(target, overwrite = true)
}
