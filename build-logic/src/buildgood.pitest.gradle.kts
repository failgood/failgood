import buildgood.PitestBuildExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension

val pitestConfig = extensions.create<PitestBuildExtension>("pitestConfig")
val commonBuild = extensions.findByType(buildgood.CommonBuildExtension::class.java)

val rootConfig =
    if (rootProject != project && rootProject.extra.has("pitestConfig")) {
        rootProject.extra["pitestConfig"] as PitestBuildExtension
    } else null

if (rootConfig != null) {
    pitestConfig.copyFrom(rootConfig)
}

afterEvaluate {
    require(commonBuild != null) { "buildgood.pitest requires buildgood.module or buildgood.root in project $path" }
    require(commonBuild.basePackage.isNotEmpty()) {
        "commonBuild.basePackage must be set to enable pitest for project $path"
    }

    pluginManager.apply("info.solidsoft.pitest")
    configure<PitestPluginExtension> {
        verbose = false
        addJUnitPlatformLauncher = false
        jvmArgs =
            listOf(
                "-Xmx512m", // necessary on CI
                "-Djava.util.logging.config.file=${rootProject.projectDir}/pitest.logging.properties",
            )
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")

        targetClasses = setOf("${commonBuild.basePackage}.*")
        targetTests =
            setOf("${commonBuild.basePackage}.*Test", "${commonBuild.basePackage}.**.*Test")
        excludedTestClasses = pitestConfig.excludedTestClasses

        pitestConfig.pitestVersion?.let { configuredPitestVersion ->
            pitestVersion.set(configuredPitestVersion)
        }

        threads =
            System.getenv("PITEST_THREADS")?.toInt()
                ?: Runtime.getRuntime().availableProcessors()
        outputFormats = setOf("XML", "HTML")
    }
}
