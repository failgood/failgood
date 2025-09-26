package buildgood

import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.jvm.toolchain.JavaLanguageVersion
import javax.inject.Inject

open class CommonBuildExtension @Inject constructor(
    objects: ObjectFactory,
    project: Project
) {
    val jvmTarget = objects.newInstance(JvmTargetConfig::class.java)
    val pitest = objects.newInstance(PitestConfig::class.java)

    internal var requireExplicitReturnTypes = false

    // Base package for the project (e.g., "failgood" or "com.christophsturm.isolationchamber")
    var basePackage: String = ""

    fun jvmTarget(action: Action<JvmTargetConfig>) {
        action.execute(jvmTarget)
    }

    fun pitest(action: Action<PitestConfig>) {
        action.execute(pitest)
    }

    fun requireExplicitReturnTypes() {
        requireExplicitReturnTypes = true
    }

    /**
     * Copies all configuration from another CommonBuildExtension (typically from root).
     * This is called before any local configuration is applied, so local config will override.
     */
    fun copyFrom(other: CommonBuildExtension) {
        // Copy JVM target settings
        jvmTarget.production = other.jvmTarget.production
        jvmTarget.test = other.jvmTarget.test

        // Copy strict Kotlin mode
        requireExplicitReturnTypes = other.requireExplicitReturnTypes

        // Copy base package
        basePackage = other.basePackage

        // Copy pitest configuration
        pitest.copyFrom(other.pitest)
    }
}

open class JvmTargetConfig @Inject constructor() {
    var production: JavaLanguageVersion = JavaLanguageVersion.of(8)
    var test: JavaLanguageVersion = JavaLanguageVersion.of(17)

    /**
     * Set production JVM target from an integer version
     */
    fun production(version: Int) {
        production = JavaLanguageVersion.of(version)
    }

    /**
     * Set test JVM target from an integer version
     */
    fun test(version: Int) {
        test = JavaLanguageVersion.of(version)
    }

    fun getProductionVersion(): String {
        return production.toString()
    }

    fun getTestVersion(): String {
        return test.toString()
    }

    /**
     * Get the JavaVersion enum for production (used by sourceCompatibility/targetCompatibility)
     */
    fun getProductionJavaVersion(): JavaVersion {
        return when (val version = production.asInt()) {
            8 -> JavaVersion.VERSION_1_8
            11 -> JavaVersion.VERSION_11
            17 -> JavaVersion.VERSION_17
            21 -> JavaVersion.VERSION_21
            else -> JavaVersion.toVersion(version)
        }
    }

    /**
     * Get the JavaVersion enum for test
     */
    fun getTestJavaVersion(): JavaVersion {
        return when (val version = test.asInt()) {
            8 -> JavaVersion.VERSION_1_8
            11 -> JavaVersion.VERSION_11
            17 -> JavaVersion.VERSION_17
            21 -> JavaVersion.VERSION_21
            else -> JavaVersion.toVersion(version)
        }
    }
}

open class PitestConfig @Inject constructor() {
    // Classes to exclude from mutation testing
    val excludedTestClasses: MutableSet<String> = mutableSetOf()

    fun excludeTestClass(pattern: String) {
        excludedTestClasses.add(pattern)
    }

    fun excludeTestClasses(vararg patterns: String) {
        excludedTestClasses.addAll(patterns)
    }

    /**
     * Copies configuration from another PitestConfig.
     */
    fun copyFrom(other: PitestConfig) {
        excludedTestClasses.clear()
        excludedTestClasses.addAll(other.excludedTestClasses)
    }
}
