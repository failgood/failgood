import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("buildgood.root")
}

// Configure build settings for all modules using the DSL
commonBuild {
    basePackage = "failgood"
    jvmTarget {
        production(JvmTarget.JVM_1_8) // JVM 1.8 for production
        test(JvmTarget.JVM_17) // JVM 17 for tests
    }
    requireExplicitReturnTypes() // Enable strict Kotlin mode
}

pitest {
    pitestVersion = libs.versions.pitest.get()
    excludeTestClasses("failgood.MultiThreadingPerformanceTest*")
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
            snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

// Task that combines runSingleNonFailgoodTest and check for CI purposes
tasks.register("ci") {
    dependsOn(gradle.includedBuild("build-logic").task(":test"))
    dependsOn(":failgood:runSingleNonFailgoodTest")
    dependsOn(":failgood:check")
    dependsOn(":failgood-android:check")
    dependsOn(":failgood-examples:check")
    dependsOn(":failgood-gradle-test:check")
    dependsOn(":experiments:failgood-android-smoke:check")
    dependsOn(":experiments:failgood-debugger:check")
    dependsOn(":experiments:gradle-plugin:check")
    dependsOn(":experiments:pitest-parser:check")
    description = "Runs exactly what runs on CI"
    group = "verification"
}
