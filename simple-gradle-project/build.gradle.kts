@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("2.1.21")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.1.3")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
