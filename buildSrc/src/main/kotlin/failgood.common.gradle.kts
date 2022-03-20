import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

plugins {
    java
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
    id("com.adarshr.test-logger")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    ktlint("com.pinterest:ktlint:0.42.1")
}

tasks {
    test {
        environment("TIMEOUT", "1000")
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
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
            languageVersion = "1.5"
            apiVersion = "1.5"
        }
    }
}
configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}
