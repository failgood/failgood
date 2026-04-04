import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { id("buildgood.root") }

group = "dev.example"

version = "1.0.0"

commonBuild {
    basePackage = "example"
    jvmTarget {
        production(JvmTarget.JVM_11)
        test(JvmTarget.JVM_17)
    }
}
