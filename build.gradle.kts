import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.ben-manes.versions") version "0.39.0"
    kotlin("jvm") version "1.5.31" apply false
    id("info.solidsoft.pitest") version "1.7.0" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("com.autonomousapps.dependency-analysis") version "0.78.0"
}


fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
    // optional parameters
    gradleReleaseChannel = "current"
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

nexusPublishing {
    repositories {
        sonatype {  //only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}

dependencyAnalysis {
    strictMode(false)
}

