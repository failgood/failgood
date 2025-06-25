@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
//    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS // kover does not work with this
}
plugins {
    id("com.autonomousapps.build-health") version "2.19.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.21" apply false
}
rootProject.name = "failgood-root"
include("failgood", "failgood-examples", "failgood-gradle-test",
    "experiments:failgood-debugger",
    "experiments:gradle-plugin"
    )
