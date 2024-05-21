@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.0.0")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
