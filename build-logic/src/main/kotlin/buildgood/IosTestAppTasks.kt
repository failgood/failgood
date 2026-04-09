package buildgood

import groovy.json.JsonSlurper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.process.ExecOperations

@DisableCachingByDefault(because = "Creates an iOS app bundle in the build directory")
abstract class CreateIosAppBundleTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val executableFile: RegularFileProperty

    @get:Input abstract val executableName: Property<String>

    @get:Input abstract val bundleIdentifier: Property<String>

    @get:Input abstract val bundleName: Property<String>

    @get:OutputDirectory abstract val appBundleDir: DirectoryProperty

    @TaskAction
    fun createBundle() {
        val appDir = appBundleDir.get().asFile
        val executable = appDir.resolve(executableName.get())
        project.delete(appDir)
        appDir.mkdirs()
        executableFile.get().asFile.copyTo(executable, overwrite = true)
        executable.setExecutable(true)
        appDir.resolve("Info.plist").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleDevelopmentRegion</key>
                <string>en</string>
                <key>CFBundleExecutable</key>
                <string>${executableName.get()}</string>
                <key>CFBundleIdentifier</key>
                <string>${bundleIdentifier.get()}</string>
                <key>CFBundleInfoDictionaryVersion</key>
                <string>6.0</string>
                <key>CFBundleName</key>
                <string>${bundleName.get()}</string>
                <key>CFBundlePackageType</key>
                <string>APPL</string>
                <key>CFBundleShortVersionString</key>
                <string>1.0</string>
                <key>CFBundleVersion</key>
                <string>1</string>
                <key>LSRequiresIPhoneOS</key>
                <true/>
            </dict>
            </plist>
            """
                .trimIndent()
        )
    }
}

@DisableCachingByDefault(because = "Runs an installed iOS simulator app test")
abstract class RunIosSimulatorAppTask
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appBundleDir: DirectoryProperty

    @get:Input abstract val bundleIdentifier: Property<String>

    @get:Input @get:Optional abstract val device: Property<String>

    @TaskAction
    fun runApp() {
        val bootedSimulator = bootedSimulator()
        val selectedDevice = device.orNull ?: bootedSimulator ?: availableSimulator()
        if (device.orNull != null || bootedSimulator == null) {
            commandIgnoringFailure("xcrun", "simctl", "boot", selectedDevice)
            command("xcrun", "simctl", "bootstatus", selectedDevice, "-b")
        }
        command(
            "xcrun",
            "simctl",
            "install",
            selectedDevice,
            appBundleDir.get().asFile.absolutePath,
        )
        command(
            "xcrun",
            "simctl",
            "launch",
            "--console",
            "--terminate-running-process",
            selectedDevice,
            bundleIdentifier.get(),
        )
    }

    private fun bootedSimulator(): String? {
        val devices =
            JsonSlurper().parseText(capture("xcrun", "simctl", "list", "devices", "booted", "-j"))
                as Map<*, *>
        val booted =
            (devices["devices"] as Map<*, *>).values
                .asSequence()
                .flatMap { (it as List<*>).asSequence() }
                .firstOrNull()
                as Map<*, *>?
        return booted?.get("udid")?.toString()
    }

    private fun availableSimulator(): String {
        val devices =
            JsonSlurper().parseText(
                capture("xcrun", "simctl", "list", "devices", "available", "-j")
            )
                as Map<*, *>
        val available =
            (devices["devices"] as Map<*, *>).values
                .asSequence()
                .flatMap { (it as List<*>).asSequence() }
                .firstOrNull() as Map<*, *>?
        return available?.get("udid")?.toString() ?: error("No available iOS simulator found.")
    }

    private fun capture(vararg command: String): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(command.toList())
            standardOutput = output
        }
        return output.toString().trim()
    }

    private fun command(vararg command: String) {
        execOperations.exec { commandLine(command.toList()) }
    }

    private fun commandIgnoringFailure(vararg command: String) {
        execOperations.exec {
            commandLine(command.toList())
            isIgnoreExitValue = true
        }
    }
}

@DisableCachingByDefault(because = "Signs, installs, and launches an iOS device app test")
abstract class RunIosDeviceAppTask
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appBundleDir: DirectoryProperty

    @get:Input abstract val bundleIdentifier: Property<String>

    @get:Input abstract val device: Property<String>

    @get:Input abstract val bundleIdentifierConfigured: Property<Boolean>

    @get:Input @get:Optional abstract val signingIdentity: Property<String>

    @get:Input abstract val provisioningProfileConfigured: Property<Boolean>

    @get:InputFile @get:Optional abstract val provisioningProfile: RegularFileProperty

    @TaskAction
    fun runApp() {
        require(bundleIdentifierConfigured.get()) {
            "Set failgood.ios.bundleIdentifier for real-device iOS runs."
        }
        require(provisioningProfileConfigured.get()) {
            "Set failgood.ios.provisioningProfile for real-device iOS runs."
        }
        val appDir = appBundleDir.get().asFile
        val entitlementsFile =
            provisioningProfile.orNull?.asFile?.let { profile ->
                profile.copyTo(appDir.resolve("embedded.mobileprovision"), overwrite = true)
                extractEntitlements(profile)
            } ?: error("Set failgood.ios.provisioningProfile for real-device iOS runs.")
        val codesignCommand =
            mutableListOf(
                "codesign",
                "--force",
                "--deep",
                "--sign",
                signingIdentity.orNull ?: firstSigningIdentity(),
                "--timestamp=none",
            )
        entitlementsFile?.let {
            codesignCommand += listOf("--entitlements", it.absolutePath)
        }
        codesignCommand += appDir.absolutePath
        command(*codesignCommand.toTypedArray())
        command(
            "xcrun",
            "devicectl",
            "device",
            "install",
            "app",
            "--device",
            device.get(),
            appDir.absolutePath,
        )
        command(
            "xcrun",
            "devicectl",
            "device",
            "process",
            "launch",
            "--console",
            "--terminate-existing",
            "--device",
            device.get(),
            bundleIdentifier.get(),
        )
    }

    private fun extractEntitlements(profile: java.io.File): java.io.File {
        val entitlementsFile = temporaryDir.resolve("ios-device-entitlements.plist")
        execOperations.exec {
            commandLine(
                "plutil",
                "-extract",
                "Entitlements",
                "xml1",
                "-o",
                entitlementsFile.absolutePath,
                "-",
            )
            standardInput =
                ByteArrayInputStream(
                    capture(
                            "openssl",
                            "smime",
                            "-inform",
                            "der",
                            "-verify",
                            "-noverify",
                            "-in",
                            profile.absolutePath,
                        )
                        .toByteArray()
                )
        }
        return entitlementsFile
    }

    private fun firstSigningIdentity(): String =
        capture("security", "find-identity", "-v", "-p", "codesigning")
            .lineSequence()
            .map(String::trim)
            .firstOrNull { VALID_IDENTITY.matches(it) }
            ?.split(Regex("\\s+"))
            ?.get(1)
            ?: error("No valid Apple code signing identity found.")

    private fun capture(vararg command: String): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(command.toList())
            standardOutput = output
        }
        return output.toString().trim()
    }

    private fun command(vararg command: String) {
        execOperations.exec { commandLine(command.toList()) }
    }

    private companion object {
        val VALID_IDENTITY = Regex("""\d+\)\s+[0-9A-F]{40}\s+".+"""")
    }
}
