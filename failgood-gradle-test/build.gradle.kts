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
    implementation("org.gradle:gradle-tooling-api:7.6")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
