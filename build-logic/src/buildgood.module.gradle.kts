import buildgood.CommonBuildExtension
import buildgood.PublishingBuildExtension
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
import com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    id("com.adarshr.test-logger")
    id("com.ncorti.ktfmt.gradle")
    kotlin("plugin.power-assert")
}

// Ensure repositories are configured
repositories { mavenCentral() }

sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}

sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}

// Create the DSL extension for this module
val commonBuild = extensions.create<CommonBuildExtension>("commonBuild")

// Create publishing extension for configuration
val publishingConfig = extensions.create<PublishingBuildExtension>("publishingConfig")

// Copy configuration from root if available
val rootConfig =
    if (rootProject != project && rootProject.extra.has("commonBuildConfig")) {
        rootProject.extra["commonBuildConfig"] as CommonBuildExtension
    } else null

if (rootConfig != null) {
    // Copy all settings from root immediately
    commonBuild.copyFrom(rootConfig)
}

// Configure power assert immediately (not in afterEvaluate)
@Suppress("OPT_IN_USAGE")
powerAssert {
    // The functions list might start empty in precompiled script plugins
    // So we need to explicitly set all the functions we want to support
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

// Apply configuration after evaluation
// At this point, any local DSL configuration will have overridden the copied values
afterEvaluate {
    val jvmConfig = commonBuild.jvmTarget

    tasks {
        test {
            useJUnitPlatform()
            outputs.upToDateWhen { false }
            testLogging {
                quiet { events = setOf(TestLogEvent.FAILED) }
                events = setOf(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = false // Only affects PASSING tests
            }
        }

        withType<JavaCompile> {
            sourceCompatibility = jvmConfig.getProductionJavaVersion().toString()
            targetCompatibility = jvmConfig.getProductionJavaVersion().toString()
        }

        withType<KotlinCompile> {
            compilerOptions {
                if (System.getenv("CI") != null) allWarningsAsErrors = true
                jvmTarget = jvmConfig.production
                freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            }
        }

        compileJava {
            sourceCompatibility = jvmConfig.getProductionJavaVersion().toString()
            targetCompatibility = jvmConfig.getProductionJavaVersion().toString()
        }

        compileTestJava {
            sourceCompatibility = jvmConfig.getTestJavaVersion().toString()
            targetCompatibility = jvmConfig.getTestJavaVersion().toString()
        }

        compileKotlin {
            compilerOptions {
                jvmTarget = jvmConfig.production
                if (commonBuild.requireExplicitReturnTypes) {
                    freeCompilerArgs.add("-XXexplicit-return-types=strict")
                }
            }
        }

        compileTestKotlin {
            compilerOptions {
                jvmTarget = jvmConfig.test
            }
        }
    }

    // Configure publishing only for modules that opt in explicitly.
    configurePublishing()
}

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
    publishingExtension.publications.create("maven", MavenPublication::class.java) {
        from(components["java"])

        artifact(
            tasks.register<Jar>("sourcesJar") {
                from(sourceSets.main.get().allSource)
                archiveClassifier = "sources"
            }
        )

        artifact(
            tasks.register<Jar>("javadocJar") {
                from(tasks.javadoc)
                archiveClassifier = "javadoc"
            }
        )

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
            sign(publishingExtension.publications["maven"])
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
