import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
}


repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    testImplementation(project(":failfast"))
    testImplementation("io.strikt:strikt-core:0.28.2")
}

tasks {
    withType<Test> { enabled = false }
}

val testMain =
    task("testMain", JavaExec::class) {
        main = "failfast.examples.AllTestsKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    main = "failfast.examples.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check { dependsOn(testMain) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        mutators.set(listOf("ALL"))
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failfast")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failfast.examples.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failfast.examples.*Test", "failfast.examples.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

