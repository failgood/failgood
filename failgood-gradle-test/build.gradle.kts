@file:Suppress("GradlePackageUpdate")

repositories {
    maven("https://repo.gradle.org/gradle/libs-releases")
    mavenCentral()
}

plugins {
    id("shared.common")
    kotlin("jvm")
}

dependencies {
    testImplementation(project(":failgood"))
    testImplementation(libs.gradle.tooling.api)
    testImplementation(libs.kotlin.stdlib.jdk8)
}

tasks { withType<Test> { useJUnitPlatform() } }
