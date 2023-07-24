import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

plugins {
    java
    kotlin("jvm")
    id("com.adarshr.test-logger")
}


tasks {
    test {
        useJUnitPlatform {
// use all engine for now because we want to see the playground engines output
        //            includeEngines = setOf("failgood")
        }
        outputs.upToDateWhen { false }
    }

    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            if (System.getenv("CI") != null)
                allWarningsAsErrors = true
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
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
