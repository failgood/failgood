package failgood.gradle

import failgood.Ignored
import failgood.Test
import failgood.testCollection
import java.io.File
import kotlin.io.path.createTempDirectory
import org.gradle.testkit.runner.GradleRunner

@Test
class AndroidManagedDeviceGradleTest {
    val tests =
        testCollection("android managed device via gradle") {
            it(
                "runs a failgood suite on a managed emulator and reports it through Gradle",
                ignored = Ignored { androidRoundtripSkipReason() },
            ) {
                val testProject = prepareAndroidConsumerProject()
                val result =
                    GradleRunner.create()
                        .withProjectDir(testProject)
                        .withArguments(":app:smokeGroupDebugAndroidTest", "--stacktrace")
                        .build()

                assert(result.output.contains("Starting 1 tests on pixel2Api34"))
                assert(
                    result.output.contains(
                        "pixel2Api34 Tests 1/1 completed. (0 skipped) (0 failed)"))

                val xmlReport =
                    File(
                            testProject,
                            "app/build/outputs/androidTest-results/managedDevice/debug/pixel2Api34")
                        .walkTopDown()
                        .firstOrNull { it.name.startsWith("TEST-") && it.extension == "xml" }
                        ?: error("Managed-device XML report not found.")
                val xml = xmlReport.readText()
                assert(xml.contains("tests=\"1\""))
                assert(xml.contains("failures=\"0\""))
                assert(xml.contains("classname=\"sample.app.AndroidRoundtripSuite\""))
                assert(xml.contains("testcase name=\"failgood suite\""))

                val htmlSummary =
                    File(
                            testProject,
                            "app/build/reports/androidTests/managedDevice/debug/allDevices/index.html",
                        )
                        .readText()
                assert(htmlSummary.contains("""<div class="counter">1</div>"""))
                assert(htmlSummary.contains("""<div class="percent">100%</div>"""))
                assert(htmlSummary.contains("sample.app.AndroidRoundtripSuite"))
            }
        }
}

private fun prepareAndroidConsumerProject(): File {
    val rootDirectory = rootDirectory()
    val tempDir = createTempDirectory("failgood-android-gradle").toFile()
    val sdkDir = checkNotNull(androidSdkDir()) { "Android SDK not found." }
    val version = projectVersion(rootDirectory)

    File(tempDir, "settings.gradle.kts")
        .writeText(
            """
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

        rootProject.name = "failgood-android-roundtrip"
        include(":app")
        includeBuild("${rootDirectory.invariantSeparatorsPath}")
        """
                .trimIndent())
    File(tempDir, "local.properties").writeText("sdk.dir=${sdkDir.invariantSeparatorsPath}\n")

    val appDir = File(tempDir, "app")
    appDir.mkdirs()
    File(appDir, "build.gradle.kts")
        .writeText(
            """
        plugins { id("com.android.application") version "9.1.0" }

        android {
            namespace = "sample.app"
            compileSdk = 35

            defaultConfig {
                applicationId = "sample.app"
                minSdk = 26
                targetSdk = 35
                testInstrumentationRunner = "failgood.android.FailgoodAndroidInstrumentationRunner"
                testInstrumentationRunnerArguments["class"] = "sample.app.AndroidRoundtripSuite"
            }

            buildFeatures { buildConfig = false }

            packaging {
                resources {
                    excludes += "META-INF/LICENSE*"
                    excludes += "META-INF/NOTICE*"
                }
            }

            testOptions {
                managedDevices {
                    localDevices {
                        create("pixel2Api34") {
                            device = "Pixel 2"
                            apiLevel = 34
                            systemImageSource = "google"
                            require64Bit = true
                        }
                    }
                    groups {
                        create("smoke") {
                            targetDevices.add(allDevices.getByName("pixel2Api34"))
                        }
                    }
                }
            }
        }

        dependencies {
            androidTestImplementation("dev.failgood:failgood-android:$version")
        }
        """
                .trimIndent())
    File(appDir, "src/main/AndroidManifest.xml").apply {
        parentFile.mkdirs()
        writeText("""<manifest />""")
    }
    File(appDir, "src/androidTest/kotlin/sample/app/AndroidRoundtripSuite.kt").apply {
        parentFile.mkdirs()
        writeText(
            """
            package sample.app

            import failgood.Test
            import failgood.testCollection

            private object SmokeState {
                @Volatile var didRun: Boolean = false
            }

            @Test
            class AndroidRoundtripSuite {
                val tests =
                    testCollection("android roundtrip", isolation = false) {
                        it("runs a passing failgood suite on device") { SmokeState.didRun = true }
                        it("proves that a test body really ran") { check(SmokeState.didRun) }
                    }
            }
            """
                .trimIndent())
    }
    return tempDir
}

private fun androidRoundtripSkipReason(): String? {
    val sdkDir = androidSdkDir() ?: return "Android SDK not found."
    if (!File(sdkDir, "emulator/emulator").exists()) {
        return "Android emulator binary not found in ${sdkDir.invariantSeparatorsPath}."
    }
    if (!File(sdkDir, "system-images/android-34/google_apis/arm64-v8a").exists()) {
        return "Missing managed-device system image android-34/google_apis/arm64-v8a."
    }
    return null
}

private fun androidSdkDir(): File? {
    val candidates =
        listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            File(System.getProperty("user.home"), "Library/Android/sdk")
                .takeIf { it.exists() }
                ?.path,
        )
    return candidates.map(::File).firstOrNull(File::exists)
}

private fun projectVersion(rootDirectory: File): String =
    File(rootDirectory, "gradle.properties")
        .readLines()
        .first { it.startsWith("version=") }
        .substringAfter('=')
        .trim()

private fun rootDirectory(): File =
    File(AndroidManagedDeviceGradleTest::class.java.protectionDomain.codeSource.location.toURI())
        .parentFile
        .parentFile
        .parentFile
        .parentFile
        .parentFile
