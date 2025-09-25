import buildgood.CommonBuildExtension

// This plugin is applied ONLY in the root build.gradle.kts
// It creates and stores the common build configuration for all submodules

// Create the extension for root-level configuration
val commonBuildConfig = extensions.create<CommonBuildExtension>("commonBuild", project)

// Store the configuration in extra properties for submodules to access
extra["commonBuildConfig"] = commonBuildConfig