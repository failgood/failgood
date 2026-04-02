@file:Suppress("GradlePackageUpdate")

plugins {
    id("buildgood.module")
    id("buildgood.pitest")
    alias(libs.plugins.kover)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(project(":failgood"))
    // it seems tools.jar is currently not necessary to compile this. I'm pretty sure that was
    // necessary at some point
    // I'm keeping it here because this is an experiment anyway.
    //    compileOnly(files("${System.getenv("java.home")}/../lib/tools.jar"))
}
