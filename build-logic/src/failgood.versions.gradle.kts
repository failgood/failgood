import failgood.versions.Versions

/**
 * Version catalog plugin that provides centralized version management.
 * Apply this plugin to access version constants in your build scripts.
 */

// Create and export the versions object
val versions = Versions()
project.extra["versions"] = versions