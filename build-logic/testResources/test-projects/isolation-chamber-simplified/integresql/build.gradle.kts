plugins { id("buildgood.module") }

dependencies {
    api(project(":core"))
    api(project(":integresql-client"))

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
