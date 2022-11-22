@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("1.7.21")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
