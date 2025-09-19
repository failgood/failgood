plugins {
    `java-gradle-plugin`
    `kotlin-dsl` apply false
    `kotlin-dsl-precompiled-script-plugins` apply false
    kotlin("jvm") version "2.1.21"
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
    implementation("org.jetbrains.kotlin.plugin.power-assert:org.jetbrains.kotlin.plugin.power-assert.gradle.plugin:$kotlinVersion")

    implementation("com.adarshr:gradle-test-logger-plugin:4.0.0")
    implementation("com.ncorti.ktfmt.gradle:plugin:0.24.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
}

// Configure flat source structure BEFORE applying kotlin-dsl
kotlin {
    sourceSets["main"].kotlin.srcDir("src")
    sourceSets["test"].kotlin.srcDir("test")
}

sourceSets.main {
    resources.srcDirs("resources")
}

sourceSets.test {
    resources.srcDirs("testResources")
}

// Apply kotlin-dsl plugin LAST (workaround for issue #21052)
// The kotlin-dsl plugin includes precompiled script plugin support
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

// to make idea ignore gradle generated classes in analyze code. (idea bug)
idea {
    module {
        generatedSourceDirs.add(File(layout.buildDirectory.get().asFile, "generated-sources/kotlin-dsl-accessors/kotlin"))
        generatedSourceDirs.add(File(layout.buildDirectory.get().asFile, "generated-sources/kotlin-dsl-plugins/kotlin"))
    }
}

tasks.register("debugPrecompiledScripts") {
    doLast {
        println("=== Debugging Precompiled Script Plugin Detection ===")

        // Check if the task exists
        val extractTask = tasks.findByName("extractPrecompiledScriptPluginPlugins")
        if (extractTask != null) {
            println("extractPrecompiledScriptPluginPlugins task exists")
            println("  Type: ${extractTask::class.simpleName}")
            println("  Enabled: ${extractTask.enabled}")

            // Try to get inputs
            extractTask.inputs.files.forEach { file ->
                println("  Input: $file")
            }
        } else {
            println("extractPrecompiledScriptPluginPlugins task NOT FOUND")
        }

        // Check what gradle.kts files are in source sets
        sourceSets["main"].kotlin.matching {
            include("**/*.gradle.kts")
        }.files.forEach { file ->
            println("Found .gradle.kts in kotlin source: $file")
        }

        // Check all source directories
        sourceSets["main"].kotlin.srcDirs.forEach { dir ->
            println("Kotlin source dir: $dir")
            if (dir.exists()) {
                dir.walkTopDown().filter { it.extension == "gradle.kts" }.forEach { file ->
                    println("  Found: ${file.relativeTo(dir)}")
                }
            }
        }
    }
}

tasks.register("showSourceSets") {
    doLast {
        sourceSets.forEach { sourceSet ->
            println("Source Set: ${sourceSet.name}")
            println("  Kotlin directories:")
            sourceSet.extensions.getByName<org.gradle.api.file.SourceDirectorySet>("kotlin").srcDirs.forEach { dir ->
                println("    - $dir (exists: ${dir.exists()})")
            }
            println("  Java directories:")
            sourceSet.java.srcDirs.forEach { dir ->
                println("    - $dir (exists: ${dir.exists()})")
            }
            println("  Resources directories:")
            sourceSet.resources.srcDirs.forEach { dir ->
                println("    - $dir (exists: ${dir.exists()})")
            }
            println()
        }
    }
}