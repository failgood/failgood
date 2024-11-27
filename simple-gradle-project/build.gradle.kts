@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.1.0")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
