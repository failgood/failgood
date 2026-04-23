plugins {
    id("buildgood.module")
    kotlin("plugin.serialization") version "2.3.21"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
