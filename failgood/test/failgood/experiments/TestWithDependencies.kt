@file:Suppress("UNUSED_PARAMETER", "unused")

package failgood.experiments

import failgood.describe

object TestWithDependencies {
    fun tests(dependency: MySlowDockerContainer) = describe { it("works") {} }

    fun otherTests(otherDependency: MySlowKafkaContainer) = describe { it("works") {} }

    class MySlowKafkaContainer

    class MySlowDockerContainer
}
