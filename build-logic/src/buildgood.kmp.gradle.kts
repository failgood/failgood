import buildgood.CommonBuildExtension
import buildgood.configureIosAppTestTasks
import buildgood.PublishingBuildExtension
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
import com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.adarshr.test-logger")
    id("com.ncorti.ktfmt.gradle")
    kotlin("plugin.power-assert")
}

repositories { mavenCentral() }

val commonBuild = extensions.create<CommonBuildExtension>("commonBuild")
val publishingConfig = extensions.create<PublishingBuildExtension>("publishingConfig")

val rootConfig =
    if (rootProject != project && rootProject.extra.has("commonBuildConfig")) {
        rootProject.extra["commonBuildConfig"] as CommonBuildExtension
    } else null

if (rootConfig != null) {
    commonBuild.copyFrom(rootConfig)
}

extensions.configure<KotlinMultiplatformExtension> {
    sourceSets.all {
        when (name) {
            "commonMain" -> kotlin.srcDir("src@common")
            "commonTest" -> kotlin.srcDir("test@common")
            "jvmMain" -> {
                kotlin.srcDir("src")
                resources.srcDir("resources")
            }
            "jvmTest" -> {
                kotlin.srcDir("test")
                resources.srcDir("testResources")
            }
            "jsMain" -> kotlin.srcDir("src@js")
            "jsTest" -> kotlin.srcDir("test@js")
            "iosMain" -> kotlin.srcDir("src@ios")
            "iosTest" -> kotlin.srcDir("test@ios")
            "wasmWasiMain" -> kotlin.srcDir("src@wasm")
            "wasmWasiTest" -> kotlin.srcDir("test@wasm")
        }
    }
}

@Suppress("OPT_IN_USAGE")
powerAssert {
    functions =
        listOf(
            "kotlin.assert",
            "kotlin.test.assertTrue",
            "kotlin.test.assertEquals",
            "kotlin.test.assertNotNull",
            "kotlin.test.assertNull",
            "kotlin.require",
            "kotlin.check",
            "failgood.softly.AssertDSL.assert",
        )
}

afterEvaluate {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            if (System.getenv("CI") != null) {
                allWarningsAsErrors = true
            }
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            freeCompilerArgs.add("-Xexpect-actual-classes")
            if (
                commonBuild.requireExplicitReturnTypes &&
                    !name.contains("Test", ignoreCase = true)
            ) {
                freeCompilerArgs.add("-XXexplicit-return-types=strict")
            }
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget =
                if (name.contains("Test", ignoreCase = true)) {
                    commonBuild.jvmTarget.test
                } else {
                    commonBuild.jvmTarget.production
                }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        val javaVersion =
            if (name.contains("Test", ignoreCase = true)) {
                commonBuild.jvmTarget.getTestJavaVersion()
            } else {
                commonBuild.jvmTarget.getProductionJavaVersion()
            }
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
        testLogging {
            quiet { events = setOf(TestLogEvent.FAILED) }
            events = setOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }

    configurePublishing()
}

configureIosAppTestTasks()

fun Project.configurePublishing() {
    if (!publishingConfig.enabled) return

    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    val pomName = publishingConfig.projectInfo.name ?: gradleProperty("project.name") ?: name
    val pomDescription =
        publishingConfig.projectInfo.description ?: gradleProperty("project.description")
    val pomUrl = publishingConfig.projectInfo.url ?: gradleProperty("project.url")
    val projectRepo = gradleProperty("project.repo")
    val scmUrl = publishingConfig.scm.url ?: projectRepo?.let { "https://github.com/$it/" }
    val scmConnection =
        publishingConfig.scm.connection ?: projectRepo?.let { "scm:git:https://github.com/$it.git" }
    val scmDeveloperConnection =
        publishingConfig.scm.developerConnection
            ?: projectRepo?.let { "scm:git:git@github.com:$it.git" }

    val publishingExtension = extensions.getByType(PublishingExtension::class.java)
    publishingExtension.publications.withType(MavenPublication::class.java).configureEach {
        pom {
            name = pomName
            if (!pomDescription.isNullOrEmpty()) {
                description = pomDescription
            }
            if (!pomUrl.isNullOrEmpty()) {
                url = pomUrl
            }

            licenses {
                license {
                    name = "The MIT License"
                    url = "https://opensource.org/licenses/MIT"
                    distribution = "repo"
                }
            }

            developers {
                developer {
                    id = "christophsturm"
                    name = "Christoph Sturm"
                    email = "me@christophsturm.com"
                }
            }

            if (
                !scmUrl.isNullOrEmpty() ||
                    !scmConnection.isNullOrEmpty() ||
                    !scmDeveloperConnection.isNullOrEmpty()
            ) {
                scm {
                    url = scmUrl
                    connection = scmConnection
                    developerConnection = scmDeveloperConnection
                }
            }
        }
    }

    extensions.configure<SigningExtension> {
        val signingKey: String? by project
        val signingPassword: String? by project
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishingExtension.publications)
        }
    }
}

fun Project.gradleProperty(name: String): String? =
    findProperty(name)?.toString()?.takeIf { it.isNotBlank() }

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
