@file:Suppress("UNUSED_PARAMETER", "SameParameterValue", "unused")

package failgood.experiments.andanotherdsl

import kotlin.reflect.KProperty

/*
 * with this test DSL we can discover tests upfront very fast without creating dependencies or executing tests
 */
class AnotherDSLExperiment {
    val tests = testCollection("collection name") {
        // these dependencies are resolved when the test runs
        val myDependency by beforeEach { MyDependency() }
        val myOtherDependency by beforeEach { MyOtherDependency(myDependency) }

        test("test name") {
            // now the dependencies are resolved
            myOtherDependency.doStuff()
            // test body
        }
        context("test group") {
            val contextDependency by beforeEach { ContextDependency(myOtherDependency) }
            test("test name") {
                // now the dependencies are resolved
                contextDependency.doStuff()
                // test body
            }

        }

    }
}

class ContextDependency(myDependency: MyOtherDependency) {
    fun doStuff() {
        TODO("Not yet implemented")
    }

}


class MyOtherDependency(myDependency: MyDependency) {
    fun doStuff() {
    }

}

class MyDependency {

}

interface MyDSL {

    fun <SubjectType> beforeEach(function: () -> SubjectType): Dependency<SubjectType> {
        TODO("Not yet implemented")
    }

    fun test(testName: String, function: () -> Unit) {
        TODO("Not yet implemented")
    }

    fun context(contextName: String, function: () -> Unit) {
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
