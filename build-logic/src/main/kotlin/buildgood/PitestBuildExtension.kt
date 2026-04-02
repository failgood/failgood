package buildgood

import javax.inject.Inject

open class PitestBuildExtension @Inject constructor() {
    // Classes to exclude from mutation testing
    val excludedTestClasses: MutableSet<String> = mutableSetOf()

    fun excludeTestClass(pattern: String) {
        excludedTestClasses.add(pattern)
    }

    fun excludeTestClasses(vararg patterns: String) {
        excludedTestClasses.addAll(patterns)
    }

    fun copyFrom(other: PitestBuildExtension) {
        excludedTestClasses.clear()
        excludedTestClasses.addAll(other.excludedTestClasses)
    }
}
