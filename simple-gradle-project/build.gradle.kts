@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("1.9.20")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.8.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
