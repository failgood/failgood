import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

plugins {
    java
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
    id("com.adarshr.test-logger")
}

dependencies {
    ktlint("com.pinterest:ktlint:0.42.1")
}

tasks {
    test {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
        // make sure the test suite works on cpu core constrained CI
        systemProperty("kotlinx.coroutines.scheduler.core.pool.size", "8")
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
configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}
