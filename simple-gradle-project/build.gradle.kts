@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm") version("1.8.22")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.8.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks {
    withType<Test> { useJUnitPlatform() }
}
