import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
    id("com.github.ben-manes.versions") version "0.36.0"
}

group = "nanotest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    testImplementation("io.strikt:strikt-core:0.28.1")
}



tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
