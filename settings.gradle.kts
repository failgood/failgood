pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
//        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
rootProject.name = "failfast-root"
include("failfast", "failfast-examples", "failfast-junit-engine-it")
