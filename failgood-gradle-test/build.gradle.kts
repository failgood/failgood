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
    testImplementation("org.gradle:gradle-tooling-api:8.12.1")
    testImplementation(kotlin("stdlib-jdk8", "2.1.0"))
}

tasks {
    withType<Test> { useJUnitPlatform() }
}

sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}
sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}
