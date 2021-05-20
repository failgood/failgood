import failgood.Versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
}


dependencies {
    testImplementation(project(":failfast"))
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:${failgood.Versions.junitPlatformVersion}")
    testImplementation("io.mockk:mockk:1.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${failgood.Versions.coroutinesVersion}")

}

tasks {
    withType<Test> { enabled = false }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
}

val testMain =
    task("testMain", JavaExec::class) {
        main = "failgood.examples.AllTestsKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    main = "failgood.examples.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check { dependsOn(testMain) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        mutators.set(listOf("ALL"))
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failgood")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failgood.examples.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failgood.examples.*Test", "failgood.examples.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

