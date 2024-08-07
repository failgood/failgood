@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.0.10")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
