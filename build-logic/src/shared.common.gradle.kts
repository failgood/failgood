import buildlogic.CommonBuildExtension
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
import com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    id("com.adarshr.test-logger")
    id("com.ncorti.ktfmt.gradle")
    kotlin("plugin.power-assert")
}

// Configure flat source structure (used by failgood and isolation-chamber)
// Restaurant uses standard Gradle structure, so it won't apply this plugin
sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}

sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}

// Create the DSL extension
val commonBuild = extensions.create<CommonBuildExtension>("commonBuild", project)

// Configure from gradle.properties
commonBuild.apply {
    jvmTarget {
        production = findProperty("jvm.production") as String? ?: "1.8"
        test = findProperty("jvm.test") as String? ?: "17"
    }
    val useFailgoodAsserts = findProperty("use.failgood.asserts")?.toString()?.toBoolean() ?: false
    val useStrictMode = findProperty("use.strict.mode")?.toString()?.toBoolean() ?: false

    if (useFailgoodAsserts) {
        useFailgoodPowerAssert()
    } else {
        useBasicPowerAssert()
    }
    if (useStrictMode) {
        useStrictKotlinMode()
    }
}

// Apply configuration after evaluation
afterEvaluate {
    val jvmConfig = commonBuild.jvmTarget

    tasks {
        test {
            useJUnitPlatform()
            outputs.upToDateWhen { false }
            testLogging {
                quiet {
                    events = setOf(TestLogEvent.FAILED)
                }
                events = setOf(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = false  // Only affects PASSING tests
            }
        }

        withType<JavaCompile> {
            sourceCompatibility = jvmConfig.getProductionVersion()
            targetCompatibility = jvmConfig.getProductionVersion()
        }

        withType<KotlinCompile> {
            compilerOptions {
                if (System.getenv("CI") != null)
                    allWarningsAsErrors = true
                jvmTarget = when (jvmConfig.getProductionVersion()) {
                    "1.8" -> JvmTarget.JVM_1_8
                    "11" -> JvmTarget.JVM_11
                    "17" -> JvmTarget.JVM_17
                    "21" -> JvmTarget.JVM_21
                    else -> JvmTarget.JVM_1_8
                }
                freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            }
        }

        compileJava {
            sourceCompatibility = jvmConfig.getProductionVersion()
            targetCompatibility = jvmConfig.getProductionVersion()
        }

        compileTestJava {
            sourceCompatibility = jvmConfig.getTestVersion()
            targetCompatibility = jvmConfig.getTestVersion()
        }

        compileKotlin {
            compilerOptions {
                jvmTarget = when (jvmConfig.getProductionVersion()) {
                    "1.8" -> JvmTarget.JVM_1_8
                    "11" -> JvmTarget.JVM_11
                    "17" -> JvmTarget.JVM_17
                    "21" -> JvmTarget.JVM_21
                    else -> JvmTarget.JVM_1_8
                }
                if (commonBuild.useStrictKotlin) {
                    freeCompilerArgs.add("-XXexplicit-return-types=strict")
                }
            }
        }

        compileTestKotlin {
            compilerOptions {
                jvmTarget = when (jvmConfig.getTestVersion()) {
                    "1.8" -> JvmTarget.JVM_1_8
                    "11" -> JvmTarget.JVM_11
                    "17" -> JvmTarget.JVM_17
                    "21" -> JvmTarget.JVM_21
                    else -> JvmTarget.JVM_17
                }
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    powerAssert {
        functions = commonBuild.powerAssertFunctions
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
    trailingCommaManagementStrategy = TrailingCommaManagementStrategy.NONE
}