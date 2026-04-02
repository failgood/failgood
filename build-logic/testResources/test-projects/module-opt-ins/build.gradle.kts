plugins { id("buildgood.root") }

group = "dev.example"

version = "1.0.0"

commonBuild {
    basePackage = "example"
    jvmTarget {
        production(17)
        test(17)
    }
    pitest { excludeTestClasses("example.DisabledMutationTest") }
}
