import buildgood.CommonBuildExtension
import buildgood.PitestBuildExtension
import info.solidsoft.gradle.pitest.PitestPlugin
import info.solidsoft.gradle.pitest.PitestPluginExtension
import info.solidsoft.gradle.pitest.PitestTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

val pitestConfig = extensions.create<PitestBuildExtension>("pitestConfig")
val commonBuild = extensions.findByType(CommonBuildExtension::class.java)

pluginManager.apply(PitestPlugin.PLUGIN_ID)

val rootConfig =
    if (rootProject != project && rootProject.extra.has("pitestConfig")) {
        rootProject.extra["pitestConfig"] as PitestBuildExtension
    } else null

if (rootConfig != null) {
    pitestConfig.copyFrom(rootConfig)
}

afterEvaluate {
    val configuredCommonBuild =
        requireNotNull(commonBuild) {
            "buildgood.pitest requires buildgood.module or buildgood.root in project $path"
        }
    require(configuredCommonBuild.basePackage.isNotEmpty()) {
        "commonBuild.basePackage must be set to enable pitest for project $path"
    }

    val targetClasses = setOf("${configuredCommonBuild.basePackage}.*")
    val targetTests =
        setOf(
            "${configuredCommonBuild.basePackage}.*Test",
            "${configuredCommonBuild.basePackage}.**.*Test",
        )
    val configuredPitestVersion = pitestConfig.pitestVersion ?: PitestPlugin.DEFAULT_PITEST_VERSION
    val pitestJvmArgs =
        listOf(
            "-Xmx512m", // necessary on CI
            "-Djava.util.logging.config.file=${rootProject.projectDir}/pitest.logging.properties",
        )

    if (pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        configureKmpPitestTask(
            pitestConfig = pitestConfig,
            configuredPitestVersion = configuredPitestVersion,
            targetClasses = targetClasses,
            targetTests = targetTests,
            pitestJvmArgs = pitestJvmArgs,
        )
    } else {
        configure<PitestPluginExtension> {
            verbose = false
            addJUnitPlatformLauncher = false
            jvmArgs = pitestJvmArgs
            avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")
            this.targetClasses = targetClasses
            this.targetTests = targetTests
            excludedTestClasses = pitestConfig.excludedTestClasses
            pitestVersion.set(configuredPitestVersion)
            threads =
                System.getenv("PITEST_THREADS")?.toInt()
                    ?: Runtime.getRuntime().availableProcessors()
            outputFormats = setOf("XML", "HTML")
        }
    }
}

fun Project.configureKmpPitestTask(
    pitestConfig: PitestBuildExtension,
    configuredPitestVersion: String,
    targetClasses: Set<String>,
    targetTests: Set<String>,
    pitestJvmArgs: List<String>,
) {
    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
    val pitestConfiguration = configurations.named(PitestPlugin.PITEST_CONFIGURATION_NAME)
    val mainKotlinSources =
        buildSet {
            kotlin.sourceSets.findByName("commonMain")?.kotlin?.srcDirs?.let(::addAll)
            kotlin.sourceSets.findByName("jvmMain")?.kotlin?.srcDirs?.let(::addAll)
        }

    dependencies.add(
        PitestPlugin.PITEST_CONFIGURATION_NAME,
        "org.pitest:pitest-command-line:$configuredPitestVersion",
    )

    tasks.register("pitest", PitestTask::class.java) {
        description = "Run PIT analysis for JVM classes"
        group = "verification"
        reportDir.set(
            layout.buildDirectory.dir("reports/${PitestPlugin.PITEST_REPORT_DIRECTORY_NAME}")
        )
        this.targetClasses.set(targetClasses)
        this.targetTests.set(targetTests)
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        excludedTestClasses.set(pitestConfig.excludedTestClasses)
        verbosity.set("NO_SPINNER")
        childProcessJvmArgs.set(pitestJvmArgs)
        outputFormats.set(setOf("XML", "HTML"))
        sourceDirs.setFrom(mainKotlinSources)
        mutableCodePaths.setFrom(layout.buildDirectory.dir("classes/kotlin/jvm/main"))
        additionalClasspath.setFrom(
            configurations.named("jvmTestRuntimeClasspath"),
            layout.buildDirectory.dir("classes/kotlin/jvm/main"),
            layout.buildDirectory.dir("classes/kotlin/jvm/test"),
            layout.buildDirectory.dir("processedResources/jvm/main"),
            layout.buildDirectory.dir("processedResources/jvm/test"),
        )
        useAdditionalClasspathFile.set(true)
        additionalClasspathFile.set(layout.buildDirectory.file("pitClasspath"))
        defaultFileForHistoryData.set(layout.buildDirectory.file("pitHistory.txt"))
        launchClasspath.setFrom(pitestConfiguration)
        dependsOn(
            "compileKotlinJvm",
            "compileTestKotlinJvm",
            "jvmProcessResources",
            "jvmTestProcessResources",
        )
    }
}
