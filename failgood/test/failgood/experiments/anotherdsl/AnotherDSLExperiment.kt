@file:Suppress("UNUSED_PARAMETER", "SameParameterValue", "unused")

package failgood.experiments.anotherdsl

import failgood.junit.ContextFinder

class AnotherDSLExperiment {
    val tests = testCollection("the context finder") {
        val contextFinder = subject { ContextFinder(runTestFixtures = true) }
        contextFinder.will("find contexts") {
            // test body
        }
    }
}
    interface MyDSL {
        fun <SubjectType> MySubject<SubjectType>.will(name: String, function: SubjectType.() -> Unit) {

        }

        fun <SubjectType> subject(function: () -> SubjectType): MySubject<SubjectType> {
            TODO("Not yet implemented")
        }

    }

interface MySubject<T> {

}

private fun testCollection(name: String, function: MyDSL.() -> Unit) {
}