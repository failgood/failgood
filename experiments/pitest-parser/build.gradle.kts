plugins {
    kotlin("jvm")
    id("failgood.common")
    application
}

dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.19.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    implementation("org.jsoup:jsoup:1.21.1")
    testImplementation(project(":failgood"))
}

sourceSets.main {
    java.srcDirs("src")
    resources.srcDirs("resources")
}

sourceSets.test {
    java.srcDirs("test")
    resources.srcDirs("testResources")
}

application { mainClass.set("CoverageReporterKt") }
