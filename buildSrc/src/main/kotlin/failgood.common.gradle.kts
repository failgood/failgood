import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    id("com.adarshr.test-logger")
    id("com.ncorti.ktfmt.gradle")
    kotlin("plugin.power-assert")

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
        compilerOptions {
            if (System.getenv("CI") != null)
                allWarningsAsErrors = true
            jvmTarget = JvmTarget.JVM_1_8
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    compileTestJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
            freeCompilerArgs.add("-XXexplicit-return-types=strict")
        }
    }

    compileTestKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}
configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}
tasks.getByName("check").dependsOn(tasks.getByName("ktfmtCheck"))
ktfmt {
    kotlinLangStyle()
    manageTrailingCommas = false
}
@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertNotNull",
        "failgood.softly.AssertDSL.assert"
    )
}

