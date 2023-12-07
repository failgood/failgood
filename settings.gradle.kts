@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.gradle.org/gradle/libs-releases") // for gradle test project
    }
//    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS // kover does not work with this
}
pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/amper/amper")
    }
}
plugins {
    id("org.jetbrains.amper.settings.plugin").version("0.1.1")
}
rootProject.name = "failgood-root"
include("failgood", "failgood-examples", "failgood-gradle-test"
    , "experiments:failgood-debugger"
    )
