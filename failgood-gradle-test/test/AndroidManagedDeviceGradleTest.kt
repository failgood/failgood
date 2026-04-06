package failgood.gradle

import failgood.Test
import failgood.testCollection
import java.io.File
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner

private const val FAILGOOD_ANDROID_VERSION = "0.9.2"
private const val MANAGED_DEVICE = "pixel2Api34"
private const val SUITE_CLASS = "sample.app.AndroidRoundtripSuite"
private const val FIRST_REPORTED_TEST =
    "AndroidRoundtripSuite: android roundtrip > runs a passing failgood suite on device"
private const val SECOND_REPORTED_TEST =
    "AndroidRoundtripSuite: android roundtrip > proves that a test body really ran"

@Test
class AndroidManagedDeviceGradleTest {
    val tests =
        testCollection("android managed device via gradle") {
            it("runs a failgood suite on a managed emulator and reports it through Gradle") {
                val projectDir = createConsumerProject()
                val result =
                    GradleRunner.create()
                        .withProjectDir(projectDir)
                        .withArguments(":app:smokeGroupDebugAndroidTest", "--stacktrace")
                        .build()

                val xmlReport =
                    File(
                            projectDir,
                            "app/build/outputs/androidTest-results/managedDevice/debug/$MANAGED_DEVICE",
                        )
                        .walkTopDown()
                        .first { it.name.startsWith("TEST-") && it.extension == "xml" }
                        .readText()
                val summaryHtmlReport =
                    File(
                            projectDir,
                            "app/build/reports/androidTests/managedDevice/debug/allDevices/index.html",
                        )
                        .readText()
                val suiteHtmlReport =
                    File(
                            projectDir,
                            "app/build/reports/androidTests/managedDevice/debug/allDevices/$SUITE_CLASS.html",
                        )
                        .readText()

                assert(xmlReport.contains("""tests="2""""))
                assert(xmlReport.contains("""failures="0""""))
                assert(xmlReport.contains("""classname="$SUITE_CLASS""""))
                assert(xmlReport.contains("""testcase name="${xmlEncode(FIRST_REPORTED_TEST)}""""))
                assert(xmlReport.contains("""testcase name="${xmlEncode(SECOND_REPORTED_TEST)}""""))

                assert(summaryHtmlReport.contains(SUITE_CLASS))
                assert(suiteHtmlReport.contains(htmlEncode(FIRST_REPORTED_TEST)))
                assert(suiteHtmlReport.contains(htmlEncode(SECOND_REPORTED_TEST)))
            }
        }
}

private fun createConsumerProject(): File {
    val projectDir =
        Files.createTempDirectory(buildDir().toPath(), "failgood-android-gradle-").toFile()
    val sdkDir = androidSdkDir()!!

    File(projectDir, "settings.gradle.kts")
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
            includeBuild("../../..")
            """
                .trimIndent())
    File(projectDir, "local.properties").writeText("sdk.dir=${sdkDir.invariantSeparatorsPath}\n")

    val appDir = File(projectDir, "app").apply { mkdirs() }
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
                    testInstrumentationRunnerArguments["class"] = "$SUITE_CLASS"
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
                            create("$MANAGED_DEVICE") {
                                device = "Pixel 2"
                                apiLevel = 34
                                systemImageSource = "google"
                                require64Bit = true
                            }
                        }
                        groups {
                            create("smoke") {
                                targetDevices.add(allDevices.getByName("$MANAGED_DEVICE"))
                            }
                        }
                    }
                }
            }

            dependencies {
                androidTestImplementation("dev.failgood:failgood-android:$FAILGOOD_ANDROID_VERSION")
            }
            """
                .trimIndent())
    File(appDir, "src/main/AndroidManifest.xml").apply {
        parentFile.mkdirs()
        writeText("<manifest />")
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
                        it("proves that a test body really ran") { assert(SmokeState.didRun) }
                    }
            }
            """
                .trimIndent())
    }
    return projectDir
}

private fun androidSdkDir(): File? =
    listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            File(System.getProperty("user.home"), "Library/Android/sdk")
                .takeIf { it.exists() }
                ?.path,
        )
        .map(::File)
        .firstOrNull(File::exists)

private fun buildDir(): File =
    File(AndroidManagedDeviceGradleTest::class.java.protectionDomain.codeSource.location.toURI())
        .parentFile
        .parentFile
        .parentFile

private fun xmlEncode(text: String): String =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun htmlEncode(text: String): String = xmlEncode(text)
