@file:Suppress("UNUSED_PARAMETER", "SameParameterValue", "unused")

package failgood.experiments.andanotherdsl

import kotlin.reflect.KProperty

class AnotherDSLExperiment {
    val tests = testCollection("collection name") {
        // these dependencies are resolved when the test runs
        val myDependency by dependency { MyDependency() }
        val myOtherDependency by dependency { MyOtherDependency(myDependency) }

        test("test name") {
            // now the dependencies are resolved
            myOtherDependency.doStuff()
            // test body
        }
    }
}


class MyOtherDependency(myDependency: MyDependency) {
    fun doStuff() {
    }

}

class MyDependency {

}

interface MyDSL {

    fun <SubjectType> dependency(function: () -> SubjectType): Dependency<SubjectType> {
        TODO("Not yet implemented")
    }

    fun test(testName: String, function: () -> Unit) {
        TODO("Not yet implemented")
    }

}

interface Dependency<T> {
    operator fun getValue(owner: Any?, property: KProperty<*>): T {
        TODO("Not yet implemented")
    }

}

private fun testCollection(name: String, function: MyDSL.() -> Unit) {
}
