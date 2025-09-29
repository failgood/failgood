import buildgood.CommonBuildExtension
import buildgood.PublishingBuildExtension
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
import com.ncorti.ktfmt.gradle.TrailingCommaManagementStrategy
import info.solidsoft.gradle.pitest.PitestPluginExtension
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
    id("info.solidsoft.pitest")
    `maven-publish`
    signing
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
val commonBuild = extensions.create<CommonBuildExtension>("commonBuild", project)

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
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertEquals",
        "kotlin.test.assertNull",
        "kotlin.require",
        "kotlin.check",
        "failgood.softly.AssertDSL.assert"
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
                jvmTarget =
                    when (jvmConfig.production.asInt()) {
                        8 -> JvmTarget.JVM_1_8
                        11 -> JvmTarget.JVM_11
                        17 -> JvmTarget.JVM_17
                        21 -> JvmTarget.JVM_21
                        else -> JvmTarget.JVM_1_8
                    }
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
                jvmTarget =
                    when (jvmConfig.production.asInt()) {
                        8 -> JvmTarget.JVM_1_8
                        11 -> JvmTarget.JVM_11
                        17 -> JvmTarget.JVM_17
                        21 -> JvmTarget.JVM_21
                        else -> JvmTarget.JVM_1_8
                    }
                if (commonBuild.requireExplicitReturnTypes) {
                    freeCompilerArgs.add("-XXexplicit-return-types=strict")
                }
            }
        }

        compileTestKotlin {
            compilerOptions {
                jvmTarget =
                    when (jvmConfig.test.asInt()) {
                        8 -> JvmTarget.JVM_1_8
                        11 -> JvmTarget.JVM_11
                        17 -> JvmTarget.JVM_17
                        21 -> JvmTarget.JVM_21
                        else -> JvmTarget.JVM_17
                    }
            }
        }
    }

    // Configure pitest if basePackage is set
    if (commonBuild.basePackage.isNotEmpty()) {
        configure<PitestPluginExtension> {
            verbose = false
            addJUnitPlatformLauncher = false
            jvmArgs =
                listOf(
                    "-Xmx512m", // necessary on CI
                    "-Djava.util.logging.config.file=${rootProject.projectDir}/pitest.logging.properties",
                )
            avoidCallsTo = setOf("kotlin.jvm.internal", "kotlin.Result")

            // Configure based on basePackage
            targetClasses = setOf("${commonBuild.basePackage}.*")
            targetTests =
                setOf("${commonBuild.basePackage}.*Test", "${commonBuild.basePackage}.**.*Test")

            // Apply excluded test classes from configuration
            excludedTestClasses = commonBuild.pitest.excludedTestClasses

            // Try to use pitest version from libs catalog if available
            pitestVersion =
                try {
                    val catalogs =
                        project.extensions.findByType<
                                org.gradle.api.artifacts.VersionCatalogsExtension
                                >()
                    if (catalogs != null && catalogs.catalogNames.contains("libs")) {
                        catalogs.named("libs").findVersion("pitest").orElse(null)?.toString()
                            ?: "1.17.1"
                    } else {
                        "1.17.1"
                    }
                } catch (e: Exception) {
                    // Use default version if catalog or version not found
                    "1.17.1"
                }

            threads =
                System.getenv("PITEST_THREADS")?.toInt()
                    ?: Runtime.getRuntime().availableProcessors()
            outputFormats = setOf("XML", "HTML")
        }
    }

    // Configure publishing if enabled via DSL
    configurePublishing()
}

fun Project.configurePublishing() {
    // Publishing configuration is set entirely via DSL

    publishing {
        publications {
            create<MavenPublication>("maven") {
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
                    if (!publishingConfig.projectInfo.name.isNullOrEmpty()) {
                        name = publishingConfig.projectInfo.name
                    }
                    if (!publishingConfig.projectInfo.description.isNullOrEmpty()) {
                        description = publishingConfig.projectInfo.description
                    }
                    if (!publishingConfig.projectInfo.url.isNullOrEmpty()) {
                        url = publishingConfig.projectInfo.url
                    }

                    licenses {
                        license {
                            name = "MIT License"
                            url = "https://opensource.org/licenses/MIT"
                        }
                    }

                    developers {
                        developer {
                            id = "christophsturm"
                            name = "Christoph Sturm"
                            email = "me@christophsturm.com"
                        }
                    }

                    if (!publishingConfig.scm.url.isNullOrEmpty()) {
                        scm {
                            url = publishingConfig.scm.url
                            connection = publishingConfig.scm.connection
                            developerConnection = publishingConfig.scm.developerConnection
                        }
                    }
                }
            }
        }
    }

    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["maven"])
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
    trailingCommaManagementStrategy = TrailingCommaManagementStrategy.NONE
}
