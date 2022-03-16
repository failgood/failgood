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
        useJUnitPlatform()
        outputs.upToDateWhen { false }
    }

    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            if (System.getenv("CI") != null) {
                allWarningsAsErrors = true
            }
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-progressive")
            languageVersion = "1.6"
            apiVersion = "1.6"
        }
    }
}
configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}
