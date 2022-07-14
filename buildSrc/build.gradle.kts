plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
    mavenCentral()
}
dependencies {
    // hotfix to make kotlin scratch files work in idea
    implementation(kotlin("script-runtime"))
    implementation(kotlin("gradle-plugin", "1.7.10"))
    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.3.0")
    implementation("com.adarshr:gradle-test-logger-plugin:3.2.0")
}

