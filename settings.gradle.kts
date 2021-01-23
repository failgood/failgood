pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
//        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
rootProject.name = "failfast-root"
include("failfast", "failfast-r2dbc", "failfast-examples")
