@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.1.21")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.2")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
