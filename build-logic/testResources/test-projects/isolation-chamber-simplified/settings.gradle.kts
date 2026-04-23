@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.jetbrains.kotlin.jvm") version "2.3.21" apply false }

dependencyResolutionManagement { repositories { mavenCentral() } }

rootProject.name = "isolation-chamber-simplified"

include("core")

include("integresql")

include("integresql-client")
