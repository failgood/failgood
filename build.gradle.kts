import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.ben-manes.versions") version "0.36.0"
    kotlin("jvm") version "1.4.21-2" apply false
    id("com.jfrog.bintray") version "1.8.5" apply false
    id("info.solidsoft.pitest") version "1.5.2" apply false
    id("tech.formatter-kt.formatter") version "0.6.15"
}

group = "com.christophsturm"
version = "0.2.0"

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val filtered =
        listOf("alpha", "beta", "rc", "cr", "m", "preview", "dev", "eap")
            .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*.*") }
    resolutionStrategy {
        componentSelection {
            all {
                if (filtered.any { it.matches(candidate.version) }) {
                    reject("Release candidate")
                }
            }
        }
        // optional parameters
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }
