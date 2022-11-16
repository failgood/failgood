@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("1.7.21")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.8.2")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
