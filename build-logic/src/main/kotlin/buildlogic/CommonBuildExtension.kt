package buildlogic

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class CommonBuildExtension @Inject constructor(
    private val objects: ObjectFactory,
    private val project: Project
) {
    val jvmTarget = objects.newInstance(JvmTargetConfig::class.java)

    internal val powerAssertFunctions = mutableListOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertNotNull"
    )

    internal var useStrictKotlin = false

    fun jvmTarget(action: Action<JvmTargetConfig>) {
        action.execute(jvmTarget)
    }

    fun useFailgoodPowerAssert() {
        powerAssertFunctions.add("failgood.softly.AssertDSL.assert")
    }

    fun useBasicPowerAssert() {
        // Already includes basic functions by default
    }

    fun useStrictKotlinMode() {
        useStrictKotlin = true
    }
}

open class JvmTargetConfig @Inject constructor() {
    var production: Any = "1.8"  // Can be String "1.8" or Int 11
    var test: Any = 17

    fun getProductionVersion(): String {
        return when (production) {
            is Int -> production.toString()
            is String -> production as String
            else -> "1.8"
        }
    }

    fun getTestVersion(): String {
        return when (test) {
            is Int -> test.toString()
            is String -> test as String
            else -> "17"
        }
    }
}