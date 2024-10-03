@file:OptIn(ExperimentalWasmDsl::class)
import org.gradle.internal.os.OperatingSystem
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport
import java.nio.file.Files
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import failgood.versions.coroutinesVersion
import failgood.versions.junitPlatformVersion
import failgood.versions.pitestVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("de.undercouch.download") version("5.6.0") apply false
    java
    kotlin("multiplatform")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
//    id("failgood.common")
//    id("failgood.publishing")
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
    id("org.jetbrains.dokka") version "2.0.0"
    kotlin("plugin.power-assert")
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("org.jetbrains.dokka") version "1.9.20"
    id("com.adarshr.test-logger")
    id("com.ncorti.ktfmt.gradle")
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
val enableJs = true
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
    wasmWasi {
        nodejs()
        binaries.executable()
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
        val wasmWasiMain by getting { kotlin.srcDir("src@wasm") }
        val wasmWasiTest by getting { kotlin.srcDir("test@wasm") }

        val jvmMain by getting {
            kotlin.srcDir("src")
            resources.srcDir("resources")
            dependencies {
                compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
//                runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")
                // to enable running test in idea without having to add the dependency manually
                api("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
                compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

                implementation(kotlin("stdlib-jdk8"))
                compileOnly("org.pitest:pitest:$pitestVersion")
                implementation("org.slf4j:slf4j-api:2.0.16")
                implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")


            }
        }
        val jvmTest by getting {
            kotlin.srcDir("test")
            resources.srcDir("testResources")
            dependencies {
                implementation("io.strikt:strikt-core:$striktVersion")
                implementation("com.christophsturm:filepeek:0.1.3") // this transitive dep is does not work in idea
                implementation("org.pitest:pitest:$pitestVersion")
                implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
                implementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
                implementation("io.projectreactor.tools:blockhound:1.0.9.RELEASE")
                implementation(kotlin("test"))
                implementation("ch.qos.logback:logback-classic:1.5.8")


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
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xexpect-actual-classes")
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
    manageTrailingCommas = false
}

// from kotlin wasm template
enum class OsName { WINDOWS, MAC, LINUX, UNKNOWN }
enum class OsArch { X86_32, X86_64, ARM64, UNKNOWN }
data class OsType(val name: OsName, val arch: OsArch)

val currentOsType = run {
    val gradleOs = OperatingSystem.current()
    val osName = when {
        gradleOs.isMacOsX -> OsName.MAC
        gradleOs.isWindows -> OsName.WINDOWS
        gradleOs.isLinux -> OsName.LINUX
        else -> OsName.UNKNOWN
    }

    val osArch = when (providers.systemProperty("sun.arch.data.model").forUseAtConfigurationTime().get()) {
        "32" -> OsArch.X86_32
        "64" -> when (providers.systemProperty("os.arch").forUseAtConfigurationTime().get().toLowerCase()) {
            "aarch64" -> OsArch.ARM64
            else -> OsArch.X86_64
        }
        else -> OsArch.UNKNOWN
    }

    OsType(osName, osArch)
}

// Deno tasks
val unzipDeno = run {
    val denoVersion = "1.38.3"
    val denoDirectory = "https://github.com/denoland/deno/releases/download/v$denoVersion"
    val denoSuffix = when (currentOsType) {
        OsType(OsName.LINUX, OsArch.X86_64) -> "x86_64-unknown-linux-gnu"
        OsType(OsName.MAC, OsArch.X86_64) -> "x86_64-apple-darwin"
        OsType(OsName.MAC, OsArch.ARM64) -> "aarch64-apple-darwin"
        else -> return@run null
    }
    val denoLocation = "$denoDirectory/deno-$denoSuffix.zip"

    val downloadedTools = File(buildDir, "tools")

    val downloadDeno = tasks.register("denoDownload", Download::class) {
        src(denoLocation)
        dest(File(downloadedTools, "deno-$denoVersion-$denoSuffix.zip"))
        overwrite(false)
    }

    tasks.register("denoUnzip", Copy::class) {
        dependsOn(downloadDeno)
        from(zipTree(downloadDeno.get().dest))
        val unpackedDir = File(downloadedTools, "deno-$denoVersion-$denoSuffix")
        into(unpackedDir)
    }
}

fun getDenoExecutableText(wasmFileName: String): String = """
import Context from "https://deno.land/std@0.201.0/wasi/snapshot_preview1.ts";

const context = new Context({
  args: Deno.args,
  env: Deno.env.toObject(),
});

const binary = await Deno.readFile("./$wasmFileName");
const module = await WebAssembly.compile(binary);
const wasmInstance = await WebAssembly.instantiate(module, {
  "wasi_snapshot_preview1": context.exports,
});

context.initialize(wasmInstance);
wasmInstance.exports.startUnitTests?.();
"""

fun Project.createDenoExecutableFile(
    taskName: String,
    wasmFileName: Provider<String>,
    outputDirectory: Provider<File>,
    resultFileName: String,
): TaskProvider<Task> = tasks.register(taskName, Task::class) {
    val denoMjs = outputDirectory.map { it.resolve(resultFileName) }
    inputs.property("wasmFileName", wasmFileName)
    outputs.file(denoMjs)

    doFirst {
        denoMjs.get().writeText(getDenoExecutableText(wasmFileName.get()))
    }
}

fun Project.createDenoExec(
    nodeMjsFile: RegularFileProperty,
    taskName: String,
    taskGroup: String?
): TaskProvider<Exec> {
    val denoFileName = "runUnitTestsDeno.mjs"

    val outputDirectory = nodeMjsFile.map { it.asFile.parentFile }
    val wasmFileName = nodeMjsFile.map { "${it.asFile.nameWithoutExtension}.wasm" }

    val denoFileTask = createDenoExecutableFile(
        taskName = "${taskName}CreateDenoFile",
        wasmFileName = wasmFileName,
        outputDirectory = outputDirectory,
        resultFileName = denoFileName
    )

    return tasks.register(taskName, Exec::class) {
        if (unzipDeno != null) {
            dependsOn(unzipDeno)
        }
        dependsOn(denoFileTask)

        taskGroup?.let {
            group = it
        }

        description = "Executes tests with Deno"

        val newArgs = mutableListOf<String>()

        executable = when (currentOsType.name) {
            OsName.WINDOWS -> "deno.exe"
            else -> unzipDeno?.let { File(unzipDeno.get().destinationDir, "deno").absolutePath } ?: "deno"
        }

        newArgs.add("run")
        newArgs.add("--allow-read")
        newArgs.add("--allow-env")

        newArgs.add(denoFileName)

        args(newArgs)
        doFirst {
            workingDir(outputDirectory)
        }
    }
}


tasks.withType<KotlinJsTest>().all {
    val denoExecTask = createDenoExec(
        inputFileProperty,
        name.replace("Node", "Deno"),
        group
    )

    denoExecTask.configure {
        dependsOn (
            project.provider { this@all.taskDependencies }
        )
    }

    tasks.withType<KotlinTestReport> {
        dependsOn(denoExecTask)
    }
}

tasks.withType<NodeJsExec>().all {
    val denoExecTask = createDenoExec(
        inputFileProperty,
        name.replace("Node", "Deno"),
        group
    )

    denoExecTask.configure {
        dependsOn (
            project.provider { this@all.taskDependencies }
        )
    }
}

// WasmEdge tasks
val wasmEdgeVersion = "0.14.0"

val wasmEdgeInnerSuffix = when (currentOsType.name) {
    OsName.LINUX -> "Linux"
    OsName.MAC -> "Darwin"
    OsName.WINDOWS -> "Windows"
    else -> error("unsupported os type $currentOsType")
}

val unzipWasmEdge = run {
    val wasmEdgeDirectory = "https://github.com/WasmEdge/WasmEdge/releases/download/$wasmEdgeVersion"
    val wasmEdgeSuffix = when (currentOsType) {
        OsType(OsName.LINUX, OsArch.X86_64) -> "manylinux_2_28_x86_64.tar.gz"
        OsType(OsName.MAC, OsArch.X86_64) -> "darwin_x86_64.tar.gz"
        OsType(OsName.MAC, OsArch.ARM64) -> "darwin_arm64.tar.gz"
        OsType(OsName.WINDOWS, OsArch.X86_32),
        OsType(OsName.WINDOWS, OsArch.X86_64) -> "windows.zip"
        else -> error("unsupported os type $currentOsType")
    }

    val artifactName = "WasmEdge-$wasmEdgeVersion-$wasmEdgeSuffix"
    val wasmEdgeLocation = "$wasmEdgeDirectory/$artifactName"

    val downloadedTools = File(buildDir, "tools")

    val downloadWasmEdge = tasks.register("wasmEdgeDownload", Download::class) {
        src(wasmEdgeLocation)
        dest(File(downloadedTools, artifactName))
        overwrite(false)
    }

    tasks.register("wasmEdgeUnzip", Copy::class) {
        dependsOn(downloadWasmEdge)

        val archive = downloadWasmEdge.get().dest

        from(if (archive.extension == "zip") zipTree(archive) else tarTree(archive))

        val currentOsTypeForConfigurationCache = currentOsType.name

        into(downloadedTools)

        doLast {
            if (currentOsTypeForConfigurationCache !in setOf(OsName.MAC, OsName.LINUX)) return@doLast

            val unzipDirectory = downloadedTools.resolve("WasmEdge-$wasmEdgeVersion-$wasmEdgeInnerSuffix")

            val libDirectory = unzipDirectory.toPath()
                .resolve(if (currentOsTypeForConfigurationCache == OsName.MAC) "lib" else "lib64")

            val targets = if (currentOsTypeForConfigurationCache == OsName.MAC)
                listOf("libwasmedge.0.1.0.dylib", "libwasmedge.0.1.0.tbd")
            else listOf("libwasmedge.so.0.1.0")

            targets.forEach {
                val target = libDirectory.resolve(it)
                val firstLink = libDirectory.resolve(it.replace("0.1.0", "0")).also(Files::deleteIfExists)
                val secondLink = libDirectory.resolve(it.replace(".0.1.0", "")).also(Files::deleteIfExists)

                Files.createSymbolicLink(firstLink, target)
                Files.createSymbolicLink(secondLink, target)
            }
        }
    }
}

fun Project.createWasmEdgeExec(
    nodeMjsFile: RegularFileProperty,
    taskName: String,
    taskGroup: String?,
    startFunction: String
): TaskProvider<Exec> {
    val outputDirectory = nodeMjsFile.map { it.asFile.parentFile }
    val wasmFileName = nodeMjsFile.map { "${it.asFile.nameWithoutExtension}.wasm" }

    return tasks.register(taskName, Exec::class) {
        dependsOn(unzipWasmEdge)
        inputs.property("wasmFileName", wasmFileName)

        taskGroup?.let { group = it }

        description = "Executes tests with WasmEdge"

        val wasmEdgeDirectory = unzipWasmEdge.get().destinationDir.resolve("WasmEdge-$wasmEdgeVersion-$wasmEdgeInnerSuffix")

        executable = wasmEdgeDirectory.resolve("bin/wasmedge").absolutePath

        doFirst {
            val newArgs = mutableListOf<String>()

            newArgs.add("--enable-gc")
            newArgs.add("--enable-exception-handling")

            newArgs.add(wasmFileName.get())
            newArgs.add(startFunction)

            args(newArgs)
            workingDir(outputDirectory)
        }
    }
}

tasks.withType<KotlinJsTest>().all {
    val wasmEdgeRunTask = createWasmEdgeExec(
        inputFileProperty,
        name.replace("Node", "WasmEdge"),
        group,
        "startUnitTests"
    )

    wasmEdgeRunTask.configure {
        dependsOn (
            project.provider { this@all.taskDependencies }
        )
    }

    tasks.withType<KotlinTestReport> {
        dependsOn(wasmEdgeRunTask)
    }
}

tasks.withType<NodeJsExec>().all {
    val wasmEdgeRunTask = createWasmEdgeExec(
        inputFileProperty,
        name.replace("Node", "WasmEdge"),
        group,
        "dummy"
    )

    wasmEdgeRunTask.configure {
        dependsOn (
            project.provider { this@all.taskDependencies }
        )
    }
}

tasks.getByName("denoUnzip").dependsOn(tasks.getByName("wasmEdgeUnzip"))
