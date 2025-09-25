plugins {
    id("buildgood.module")
    application
}

dependencies {
    implementation(libs.bundles.jackson)
    implementation(libs.jsoup)
    testImplementation(project(":failgood"))
}

application { mainClass.set("CoverageReporterKt") }
