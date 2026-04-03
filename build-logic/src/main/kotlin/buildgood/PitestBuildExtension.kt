package buildgood

import javax.inject.Inject

open class PitestBuildExtension @Inject constructor() {
    var pitestVersion: String? = null

    // Classes to exclude from mutation testing
    val excludedTestClasses: MutableSet<String> = mutableSetOf()

    fun excludeTestClass(pattern: String) {
        excludedTestClasses.add(pattern)
    }

    fun excludeTestClasses(vararg patterns: String) {
        excludedTestClasses.addAll(patterns)
    }

    fun copyFrom(other: PitestBuildExtension) {
        pitestVersion = other.pitestVersion
        excludedTestClasses.clear()
        excludedTestClasses.addAll(other.excludedTestClasses)
    }
}
