@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}
rootProject.name = "failgood-root"
include("failfast", "failfast-examples", "failfast-junit-engine-it")
