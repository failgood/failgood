@file:Suppress("GradlePackageUpdate")

repositories {
    maven("https://repo.gradle.org/gradle/libs-releases")
    mavenCentral()
}
plugins {
    id("failgood.common")
    kotlin("jvm")
    id("org.jmailen.kotlinter")
}

dependencies {
    testImplementation(project(":failgood"))
    testImplementation("org.gradle:gradle-tooling-api:7.6")
    testImplementation(kotlin("stdlib-jdk8"))
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
