@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.0.20")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.1")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
