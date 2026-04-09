package buildgood

import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

fun Project.configureIosAppTestTasks() {
    afterEvaluate {
        val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
        if (!kotlin.targets.names.containsAll(listOf("iosArm64", "iosSimulatorArm64"))) {
            return@afterEvaluate
        }

        val isMacOs = System.getProperty("os.name").contains("Mac", ignoreCase = true)
        val configuredIosBundleIdentifier =
            providers.gradleProperty("failgood.ios.bundleIdentifier")
        val iosTestBundleIdentifier = configuredIosBundleIdentifier.orElse(defaultBundleIdentifier())
        val configuredIosProvisioningProfile =
            providers.gradleProperty("failgood.ios.provisioningProfile")
        val configuredIosSigningIdentity =
            providers.gradleProperty("failgood.ios.signingIdentity")
        val configuredIosTestDevice = providers.gradleProperty("failgood.ios.testDevice")
        val iosTestBundleName = defaultBundleName()
        val wantsAutomaticIosAppTest =
            gradle.startParameter.taskNames.any {
                it == "iosAppTest" || it.endsWith(":iosAppTest")
            }
        val iosAppTestDevice =
            if (wantsAutomaticIosAppTest) {
                configuredIosTestDevice.orNull?.takeIf { isAvailableIosDevice(it, isMacOs) }
            } else {
                null
            }

        val simulatorTestApp =
            tasks.register<CreateIosAppBundleTask>("packageIosSimulatorArm64TestApp") {
                val linkTask = tasks.named<KotlinNativeLink>("linkDebugTestIosSimulatorArm64")
                group = "verification"
                description =
                    "Packages the iosSimulatorArm64 native test executable as an installable app"
                dependsOn(linkTask)
                executableFile.set(layout.file(linkTask.flatMap { it.outputFile }))
                executableName.set(iosTestBundleName)
                bundleIdentifier.set(iosTestBundleIdentifier)
                bundleName.set(iosTestBundleName)
                appBundleDir.set(
                    layout.buildDirectory.dir(
                        "iosTestApp/iosSimulatorArm64/$iosTestBundleName.app"
                    ))
                onlyIf { isMacOs }
            }

        tasks.register<RunIosSimulatorAppTask>("iosSimulatorArm64AppTest") {
            group = "verification"
            description =
                "Installs and launches the iosSimulatorArm64 native test app on a simulator"
            dependsOn(simulatorTestApp)
            appBundleDir.set(simulatorTestApp.flatMap { it.appBundleDir })
            bundleIdentifier.set(iosTestBundleIdentifier)
            providers.gradleProperty("failgood.ios.simulator.device").orNull?.let(device::set)
            onlyIf { isMacOs }
        }

        val deviceTestApp =
            tasks.register<CreateIosAppBundleTask>("packageIosArm64TestApp") {
                val linkTask = tasks.named<KotlinNativeLink>("linkDebugTestIosArm64")
                group = "verification"
                description = "Packages the iosArm64 native test executable as an installable app"
                dependsOn(linkTask)
                executableFile.set(layout.file(linkTask.flatMap { it.outputFile }))
                executableName.set(iosTestBundleName)
                bundleIdentifier.set(iosTestBundleIdentifier)
                bundleName.set(iosTestBundleName)
                appBundleDir.set(
                    layout.buildDirectory.dir("iosTestApp/iosArm64/$iosTestBundleName.app")
                )
                onlyIf { isMacOs }
            }

        tasks.register<RunIosDeviceAppTask>("iosArm64AppTest") {
            group = "verification"
            description =
                "Signs, installs, and launches the iosArm64 native test app on a connected device"
            dependsOn(deviceTestApp)
            appBundleDir.set(deviceTestApp.flatMap { it.appBundleDir })
            bundleIdentifier.set(iosTestBundleIdentifier)
            bundleIdentifierConfigured.set(
                configuredIosBundleIdentifier.map { true }.orElse(false)
            )
            device.set(configuredIosTestDevice)
            provisioningProfileConfigured.set(
                configuredIosProvisioningProfile.map { true }.orElse(false)
            )
            configuredIosSigningIdentity.orNull?.let(signingIdentity::set)
            configuredIosProvisioningProfile.orNull?.let { provisioningProfile.set(file(it)) }
            onlyIf { isMacOs }
        }

        tasks.register("iosAppTest") {
            group = "verification"
            description =
                "Runs the packaged iOS failgood tests on a configured device or falls back to a simulator"
            dependsOn(
                if (iosAppTestDevice != null) {
                    "iosArm64AppTest"
                } else {
                    "iosSimulatorArm64AppTest"
                }
            )
            onlyIf { isMacOs }
        }
    }
}

private fun Project.defaultBundleIdentifier(): String =
    "dev.failgood.${name.replace('-', '.')}.ios.test"

private fun Project.defaultBundleName(): String =
    name.split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) } + "IosTest"

private fun Project.isAvailableIosDevice(device: String, isMacOs: Boolean): Boolean {
    if (!isMacOs) return false
    val process =
        ProcessBuilder("xcrun", "devicectl", "list", "devices", "--columns", "*")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
    val output = process.inputStream.bufferedReader().readText()
    if (process.waitFor() != 0) return false
    return output.lineSequence().any {
        it.contains(device) && (it.contains("connected") || it.contains("available"))
    }
}
