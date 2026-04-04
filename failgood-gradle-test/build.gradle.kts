@file:Suppress("GradlePackageUpdate")

repositories {
    maven("https://repo.gradle.org/gradle/libs-releases")
    mavenCentral()
}

plugins { id("buildgood.module") }

dependencies {
    testImplementation(project(":failgood"))
    testImplementation(gradleTestKit())
    testImplementation(libs.gradle.tooling.api)
    testImplementation(libs.kotlin.stdlib.jdk8)
}

tasks { withType<Test> { useJUnitPlatform() } }
