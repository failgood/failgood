package buildgood

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

open class CommonBuildExtension
@Inject
constructor(objects: ObjectFactory) {
    val jvmTarget = objects.newInstance(JvmTargetConfig::class.java)

    internal var requireExplicitReturnTypes = false
    var basePackage: String = ""

    fun jvmTarget(action: Action<JvmTargetConfig>) {
        action.execute(jvmTarget)
    }

    fun requireExplicitReturnTypes() {
        requireExplicitReturnTypes = true
    }

    /**
     * Copies all configuration from another CommonBuildExtension (typically from root). This is
     * called before any local configuration is applied, so local config will override.
     */
    fun copyFrom(other: CommonBuildExtension) {
        // Copy JVM target settings
        jvmTarget.production = other.jvmTarget.production
        jvmTarget.test = other.jvmTarget.test

        // Copy strict Kotlin mode
        requireExplicitReturnTypes = other.requireExplicitReturnTypes
        basePackage = other.basePackage
    }
}

open class JvmTargetConfig @Inject constructor() {
    var production: JvmTarget = JvmTarget.DEFAULT
    var test: JvmTarget = JvmTarget.JVM_17

    fun production(version: JvmTarget) {
        production = version
    }

    fun test(version: JvmTarget) {
        test = version
    }

    fun getProductionVersion(): String {
        return production.target
    }

    fun getTestVersion(): String {
        return test.target
    }

    /** Get the JavaVersion enum for production (used by sourceCompatibility/targetCompatibility) */
    fun getProductionJavaVersion(): JavaVersion {
        return JavaVersion.toVersion(production.target)
    }

    /** Get the JavaVersion enum for test */
    fun getTestJavaVersion(): JavaVersion {
        return JavaVersion.toVersion(test.target)
    }
}
