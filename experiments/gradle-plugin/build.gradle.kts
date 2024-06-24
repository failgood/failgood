@file:Suppress("GradlePackageUpdate")

import failgood.versions.junitJupiterVersion
import failgood.versions.junitPlatformVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension


plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("failgood.common")
//    id("failgood.publishing")
    id("org.jetbrains.kotlinx.kover") version "0.8.1"
    id("org.jetbrains.dokka") version "1.9.20"
    `java-gradle-plugin`

}

gradlePlugin {
    plugins {
        create("customTestPlugin") {
            id = "failgood.gradle.FailgoodPlugin"
            implementationClass = "failgood.gradle.FailgoodPlugin"
        }
    }
}

// Modify the test task to depend on the metadata generation
tasks.test {
    // Ensure the test task depends on pluginUnderTestMetadata
    dependsOn(tasks.pluginUnderTestMetadata)

    // Add the metadata to the test classpath
    doFirst {
        classpath = classpath + files(tasks.pluginUnderTestMetadata.get().outputDirectory)
    }
    useJUnitPlatform()

    // Add the metadata to the test classpath
}
dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":failgood"))
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    implementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    implementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")


}
sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}
sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}
plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        addJUnitPlatformLauncher = false
        jvmArgs = listOf("-Xmx512m") // necessary on CI
        avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")
        targetClasses = setOf("failgood.*") // by default "${project.group}.*"
        targetTests = setOf("failgood.*Test", "failgood.**.*Test")
        pitestVersion = failgood.versions.pitestVersion
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}
@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
