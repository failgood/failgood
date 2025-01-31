@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.1.10")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
