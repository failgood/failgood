import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    ktlint("com.pinterest:ktlint:0.42.1")
}

tasks {
    test {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
    }

    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-progressive")
            languageVersion = "1.6"
            apiVersion = "1.6"
        }
    }
}
