@file:Suppress("GradlePackageUpdate")

repositories {
    maven("https://repo.gradle.org/gradle/libs-releases")
    mavenCentral()
}
plugins {
    id("failgood.common")
    kotlin("jvm")
}

dependencies {
    testImplementation(project(":failgood"))
    testImplementation("org.gradle:gradle-tooling-api:8.4")
    testImplementation(kotlin("stdlib-jdk8"))
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
