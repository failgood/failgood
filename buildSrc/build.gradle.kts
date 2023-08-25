plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
    idea
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
    mavenCentral()
}
dependencies {
    // hotfix to make kotlin scratch files work in idea
    implementation(kotlin("script-runtime"))
    implementation(kotlin("gradle-plugin", "1.9.10"))
    implementation("com.adarshr:gradle-test-logger-plugin:3.2.0")
}
idea {
    module {
        generatedSourceDirs.add(File(buildDir, "generated-sources/kotlin-dsl-accessors/kotlin"))
        generatedSourceDirs.add(File(buildDir, "generated-sources/kotlin-dsl-plugins/kotlin"))
    }
}
