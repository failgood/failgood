import buildgood.CommonBuildExtension
import buildgood.PublishingBuildExtension
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
    `maven-publish`
    signing
}

// Configure flat source structure (used by failgood and isolation-chamber)
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

// First, copy configuration from root if available
val rootConfig = if (rootProject.extra.has("commonBuildConfig")) {
    rootProject.extra["commonBuildConfig"] as CommonBuildExtension
} else null

if (rootConfig != null) {
    // Copy all settings from root immediately
    commonBuild.copyFrom(rootConfig)
} else {
    // Fallback to gradle.properties for backwards compatibility
    commonBuild.apply {
        jvmTarget {
            val prodVersion = findProperty("jvm.production") as String?
            val testVersion = findProperty("jvm.test") as String?
            if (prodVersion != null) {
                production(prodVersion.replace("1.", "").toIntOrNull() ?: 8)
            }
            if (testVersion != null) {
                test(testVersion.replace("1.", "").toIntOrNull() ?: 17)
            }
        }
        val useFailgoodAsserts = findProperty("use.failgood.asserts")?.toString()?.toBoolean()
        val useStrictMode = findProperty("use.strict.mode")?.toString()?.toBoolean()

        if (useFailgoodAsserts == true) {
            useFailgoodPowerAssert()
        }
        if (useStrictMode == true) {
            useStrictKotlinMode()
        }
    }
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
                quiet {
                    events = setOf(TestLogEvent.FAILED)
                }
                events = setOf(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = false  // Only affects PASSING tests
            }
        }

        withType<JavaCompile> {
            sourceCompatibility = jvmConfig.getProductionJavaVersion().toString()
            targetCompatibility = jvmConfig.getProductionJavaVersion().toString()
        }

        withType<KotlinCompile> {
            compilerOptions {
                if (System.getenv("CI") != null)
                    allWarningsAsErrors = true
                jvmTarget = when (jvmConfig.production.asInt()) {
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
                jvmTarget = when (jvmConfig.production.asInt()) {
                    8 -> JvmTarget.JVM_1_8
                    11 -> JvmTarget.JVM_11
                    17 -> JvmTarget.JVM_17
                    21 -> JvmTarget.JVM_21
                    else -> JvmTarget.JVM_1_8
                }
                if (commonBuild.useStrictKotlin) {
                    freeCompilerArgs.add("-XXexplicit-return-types=strict")
                }
            }
        }

        compileTestKotlin {
            compilerOptions {
                jvmTarget = when (jvmConfig.test.asInt()) {
                    8 -> JvmTarget.JVM_1_8
                    11 -> JvmTarget.JVM_11
                    17 -> JvmTarget.JVM_17
                    21 -> JvmTarget.JVM_21
                    else -> JvmTarget.JVM_17
                }
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    powerAssert {
        functions = commonBuild.powerAssertFunctions
    }

    // Configure publishing if enabled via DSL
    configurePublishing()
}

fun Project.configurePublishing() {
    // Read project metadata from gradle.properties
    val projectName = findProperty("project.name") as String? ?: project.name
    val projectDescription = findProperty("project.description") as String? ?: ""
    val projectUrl = findProperty("project.url") as String? ?: ""
    val projectRepo = findProperty("project.repo") as String? ?: ""

    // Apply any DSL configuration
    publishingConfig.apply {
        if (projectInfo.name.isNullOrEmpty()) {
            projectInfo {
                name = projectName
                description = projectDescription
                url = projectUrl
            }
        }
        if (scm.url.isNullOrEmpty() && projectRepo.isNotEmpty()) {
            scm {
                fromGitHub(projectRepo)
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                artifact(tasks.register<Jar>("sourcesJar") {
                    from(sourceSets.main.get().allSource)
                    archiveClassifier = "sources"
                })

                artifact(tasks.register<Jar>("javadocJar") {
                    from(tasks.javadoc)
                    archiveClassifier = "javadoc"
                })

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