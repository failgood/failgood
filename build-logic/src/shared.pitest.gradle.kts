import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("info.solidsoft.pitest")
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        verbose = false
        addJUnitPlatformLauncher = false
        jvmArgs = listOf(
            "-Xmx512m", // necessary on CI
            "-Djava.util.logging.config.file=${rootProject.projectDir}/pitest.logging.properties"
        )
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")

        // Read target classes pattern from gradle.properties
        val pitestTargetClasses = project.findProperty("pitest.target.classes") as String?
            ?: "${project.group}.*"
        targetClasses = setOf(pitestTargetClasses)

        // Read target tests pattern from gradle.properties
        val pitestTargetTests = project.findProperty("pitest.target.tests") as String?
            ?: "*Test,**.*Test"
        targetTests = pitestTargetTests.split(",").map { it.trim() }.toSet()

        // Read pitest version from gradle.properties
        val pitestVer = project.findProperty("pitest.version") as String?
            ?: "1.17.1"
        pitestVersion = pitestVer

        threads = System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        outputFormats = setOf("XML", "HTML")
    }
}