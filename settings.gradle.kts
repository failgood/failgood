@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
//    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS // kover does not work with this
}
plugins {
    id("com.autonomousapps.build-health") version "3.0.3"
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
}
rootProject.name = "failgood-root"
includeBuild("build-logic")
include("failgood", "failgood-android", "failgood-examples", "failgood-gradle-test",
    "experiments:failgood-android-smoke",
    "experiments:failgood-debugger",
    "experiments:gradle-plugin",
    "experiments:pitest-parser"
    )
