import buildgood.CommonBuildExtension
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins { id("com.github.ben-manes.versions") }

// Create the root configuration extension
val commonBuildConfig = extensions.create<CommonBuildExtension>("commonBuild", project)

// Store the configuration in extra properties for submodules to access
extra["commonBuildConfig"] = commonBuildConfig

// Function to check if a version is non-stable
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// Configure dependency updates task
tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
    // optional parameters
    gradleReleaseChannel = "current"
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

// Configure wrapper task if it exists (only in root projects)
tasks.findByName("wrapper")?.let {
    if (it is org.gradle.api.tasks.wrapper.Wrapper) {
        it.distributionType = org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
    }
}
