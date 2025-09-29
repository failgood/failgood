plugins {
    `java-gradle-plugin`
    `kotlin-dsl` apply false
    `kotlin-dsl-precompiled-script-plugins` apply false
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.power-assert") version "2.1.21"
    id("com.ncorti.ktfmt.gradle") version "0.24.0"
    id("com.adarshr.test-logger") version "4.0.0"
    idea
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
    mavenCentral()
}

val kotlinVersion = "2.1.21"

dependencies {
    // hotfix to make kotlin scratch files work in idea
    implementation(kotlin("script-runtime"))
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(
        "org.jetbrains.kotlin.plugin.power-assert:org.jetbrains.kotlin.plugin.power-assert.gradle.plugin:$kotlinVersion"
    )

    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.ncorti.ktfmt.gradle:plugin:0.24.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
    implementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.15.0")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.52.0")

    // Test dependencies
    testImplementation(gradleTestKit())
    testImplementation("dev.failgood:failgood:0.9.1")
    testImplementation(kotlin("test"))
}

// Configure flat source structure BEFORE applying kotlin-dsl
kotlin {
    sourceSets["main"].kotlin.srcDir("src")
    sourceSets["test"].kotlin.srcDir("test")
}

sourceSets.main { resources.srcDirs("resources") }

sourceSets.test { resources.srcDirs("testResources") }

// Apply kotlin-dsl plugin LAST (workaround for issue #21052)
// The kotlin-dsl plugin includes precompiled script plugin support
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Configure test-logger plugin
testlogger {
    theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}

// Configure ktfmt for build-logic itself
ktfmt { kotlinLangStyle() }

// Register a custom ktfmt task for test fixtures
tasks.register<com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask>("formatTestFixtures") {
    description = "Format Kotlin files in test fixtures"
    group = "formatting"
    source =
        fileTree("testResources/test-projects") {
            include("**/*.kt")
            include("**/*.gradle.kts")
        }
}

// Make ktfmtFormat depend on formatting test fixtures
tasks.named("ktfmtFormat") { dependsOn("formatTestFixtures") }

// to make idea ignore gradle generated classes in analyze code. (idea bug)
idea {
    module {
        generatedSourceDirs.add(
            File(
                layout.buildDirectory.get().asFile,
                "generated-sources/kotlin-dsl-accessors/kotlin",
            )
        )
        generatedSourceDirs.add(
            File(layout.buildDirectory.get().asFile, "generated-sources/kotlin-dsl-plugins/kotlin")
        )
    }
}
