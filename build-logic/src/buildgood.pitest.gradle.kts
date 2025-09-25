import buildgood.CommonBuildExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("info.solidsoft.pitest")
}

afterEvaluate {
    // Get the common build configuration
    val commonBuild = extensions.findByType<CommonBuildExtension>()
    if (commonBuild == null || commonBuild.basePackage.isEmpty()) {
        logger.warn("Pitest plugin applied but basePackage not configured in commonBuild")
        return@afterEvaluate
    }

    configure<PitestPluginExtension> {
        verbose = false
        addJUnitPlatformLauncher = false
        jvmArgs = listOf(
            "-Xmx512m", // necessary on CI
            "-Djava.util.logging.config.file=${rootProject.projectDir}/pitest.logging.properties"
        )
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")

        // Configure based on basePackage
        targetClasses = setOf("${commonBuild.basePackage}.*")
        targetTests = setOf(
            "${commonBuild.basePackage}.*Test",
            "${commonBuild.basePackage}.**.*Test"
        )

        // Apply excluded test classes from configuration
        excludedTestClasses = commonBuild.pitest.excludedTestClasses

        // Use pitest version from libs if available
        val libs = project.extensions.findByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
            ?.named("libs")
        if (libs != null) {
            try {
                pitestVersion = libs.findVersion("pitest").get().toString()
            } catch (e: Exception) {
                // Use default version if not found in version catalog
            }
        }

        threads = System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        outputFormats = setOf("XML", "HTML")
    }
}