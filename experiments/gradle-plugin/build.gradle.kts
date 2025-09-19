@file:Suppress("GradlePackageUpdate")

import failgood.versions.Versions
import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    id("failgood.versions")
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("failgood.common")
    //    id("failgood.publishing")
    id("org.jetbrains.kotlinx.kover") version "0.9.2"
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
    doFirst { classpath += files(tasks.pluginUnderTestMetadata.get().outputDirectory) }
    useJUnitPlatform()

    // Don't fail when no tests are found (test is commented out)
    // this is necessary in gradle 9.0 and not supported in gradle 8.x so we keep it commented out
    // for now
    //    failOnNoDiscoveredTests = false
}

// Access versions object from the versions plugin
val versions: Versions by project.extra

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":failgood"))
    implementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    implementation("org.junit.jupiter:junit-jupiter-api:${versions.junitJupiter}")
    implementation("org.junit.jupiter:junit-jupiter-engine:${versions.junitJupiter}")
    implementation("org.junit.platform:junit-platform-launcher:${versions.junitPlatform}")
    implementation("org.junit.platform:junit-platform-engine:${versions.junitPlatform}")
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
        pitestVersion = versions.pitest
        threads =
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()

        outputFormats = setOf("XML", "HTML")
    }
}

@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertNotNull")
}
