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
}

