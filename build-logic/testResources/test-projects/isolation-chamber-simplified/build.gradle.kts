import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { id("buildgood.root") }

allprojects {
    group = "com.test.isolationchamber"
    version = "1.0.0-TEST"

    repositories { mavenCentral() }
}

// Configure build settings for all modules using the DSL
commonBuild {
    basePackage = "com.test.isolationchamber"
    jvmTarget {
        production(JvmTarget.JVM_11)
        test(JvmTarget.JVM_17)
    }
}
