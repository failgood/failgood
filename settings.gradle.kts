pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "failfast-root"
include("failfast", "failfast-examples", "failfast-junit-engine-it")
