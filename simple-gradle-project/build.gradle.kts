@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("1.9.10")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.8.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
