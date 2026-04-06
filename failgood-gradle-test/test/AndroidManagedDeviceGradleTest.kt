package failgood.gradle

import failgood.Test
import failgood.testCollection
import java.io.File
import java.nio.file.Files
import org.gradle.testkit.runner.GradleRunner

private const val FAILGOOD_ANDROID_VERSION = "0.9.2"
private const val MANAGED_DEVICE = "pixel2Api34"
private const val FIRST_SUITE_CLASS = "sample.app.AndroidRoundtripSuite"
private const val SECOND_SUITE_CLASS = "sample.app.SecondRoundtripSuite"
private const val FIRST_REPORTED_TEST =
    "AndroidRoundtripSuite: android roundtrip > runs a passing failgood suite on device"
private const val SECOND_REPORTED_TEST =
    "AndroidRoundtripSuite: android roundtrip > proves that a test body really ran"
private const val THIRD_REPORTED_TEST =
    "SecondRoundtripSuite: second roundtrip > discovers a second suite without explicit class selection"

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
                            "app/build/outputs/androidTest-results/managedDevice/debug/$MANAGED_DEVICE")
                        .walkTopDown()
                        .filter { it.name.startsWith("TEST-") && it.extension == "xml" }
                        .joinToString("\n") { it.readText() }
                val summaryHtmlReport =
                    File(
                            projectDir,
                            "app/build/reports/androidTests/managedDevice/debug/allDevices/index.html",
                        )
                        .readText()
                val firstSuiteHtmlReport =
                    File(
                            projectDir,
                            "app/build/reports/androidTests/managedDevice/debug/allDevices/$FIRST_SUITE_CLASS.html",
                        )
                        .readText()
                val secondSuiteHtmlReport =
                    File(
                            projectDir,
                            "app/build/reports/androidTests/managedDevice/debug/allDevices/$SECOND_SUITE_CLASS.html",
                        )
                        .readText()

                assert(result.output.contains("BUILD SUCCESSFUL"))
                assert(xmlReport.contains("""classname="$FIRST_SUITE_CLASS""""))
                assert(xmlReport.contains("""classname="$SECOND_SUITE_CLASS""""))
                assert(xmlReport.contains("""testcase name="${xmlEncode(FIRST_REPORTED_TEST)}""""))
                assert(xmlReport.contains("""testcase name="${xmlEncode(SECOND_REPORTED_TEST)}""""))
                assert(xmlReport.contains("""testcase name="${xmlEncode(THIRD_REPORTED_TEST)}""""))

                assert(summaryHtmlReport.contains(FIRST_SUITE_CLASS))
                assert(summaryHtmlReport.contains(SECOND_SUITE_CLASS))
                assert(firstSuiteHtmlReport.contains(htmlEncode(FIRST_REPORTED_TEST)))
                assert(firstSuiteHtmlReport.contains(htmlEncode(SECOND_REPORTED_TEST)))
                assert(secondSuiteHtmlReport.contains(htmlEncode(THIRD_REPORTED_TEST)))
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
    File(appDir, "src/androidTest/kotlin/sample/app/SecondRoundtripSuite.kt").apply {
        parentFile.mkdirs()
        writeText(
            """
            package sample.app

            import failgood.Test
            import failgood.testCollection

            @Test
            class SecondRoundtripSuite {
                val tests =
                    testCollection("second roundtrip") {
                        it("discovers a second suite without explicit class selection") { assert(true) }
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
