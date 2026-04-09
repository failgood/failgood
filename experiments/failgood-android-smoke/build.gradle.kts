plugins { id("com.android.application") }

import java.io.ByteArrayOutputStream

val smokeInstrumentation = "failgood.android.FailgoodAndroidInstrumentationRunner"

android {
    namespace = "failgood.android.smoke"
    compileSdk = 35

    defaultConfig {
        applicationId = "failgood.android.smoke"
        minSdk = 26
        targetSdk = 35
        testApplicationId = "failgood.android.smoke.test"
        testInstrumentationRunner = smokeInstrumentation
        testInstrumentationRunnerArguments["class"] = "failgood.android.smoke.AndroidSmokeSuite"
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

dependencies { androidTestImplementation(project(":failgood-android")) }

tasks.register("runFailgoodAndroidSmoke") {
    group = "verification"
    description = "Installs and runs the failgood Android smoke instrumentation on a connected device"
    dependsOn("installDebug", "installDebugAndroidTest")

    doLast {
        val devicesOutput = runCommand("adb", "devices")
        val connectedDevices =
            devicesOutput
                .lineSequence()
                .map(String::trim)
                .filter { it.endsWith("\tdevice") }
                .toList()
        check(connectedDevices.isNotEmpty()) {
            "No connected adb device found. Start an emulator or attach a device first."
        }

        val text =
            runCommand(
                "adb",
                "shell",
                "am",
                "instrument",
                "-w",
                "failgood.android.smoke.test/$smokeInstrumentation",
            )
        println(text)
        check(text.contains("FAILGOOD_ANDROID_OK")) {
            "Smoke instrumentation did not report the failgood Android success marker."
        }
        check(text.contains("INSTRUMENTATION_CODE: -1")) {
            "Instrumentation did not finish successfully."
        }
    }
}

tasks.register("runManagedFailgoodAndroidSmoke") {
    group = "verification"
    description = "Runs the failgood Android smoke instrumentation on an AGP managed emulator"
    dependsOn("smokeGroupDebugAndroidTest")
}

fun runCommand(vararg command: String): String {
    val output = ByteArrayOutputStream()
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    process.inputStream.copyTo(output)
    val exitCode = process.waitFor()
    check(exitCode == 0) { "${command.joinToString(" ")} failed with exit code $exitCode" }
    return output.toString()
}
