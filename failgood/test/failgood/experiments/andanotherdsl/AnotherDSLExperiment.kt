@file:Suppress("UNUSED_PARAMETER", "SameParameterValue", "unused", "RemoveEmptyClassBody")

package failgood.experiments.andanotherdsl

import failgood.FailGoodException
import failgood.Test
import failgood.experiments.andanotherdsl.Node.*
import failgood.tests
import kotlinx.coroutines.delay
import kotlin.reflect.KProperty
import kotlin.test.assertEquals

/**
 * with this test DSL we can discover tests upfront very fast without creating dependencies or executing tests.
 * To get all test names we just execute the test collection lambda, without executing the beforeEach lambdas
 * The for each text we execute just the path to this test (like the [failgood.internal.SingleTestExecutor] does)
 *
 * Or another possibility would be to just compile a list of all the beforeEach and test lambdas and then execute those for each test
 * but then the references to the dependencies would become more complex and need to resolve to different values depending on the test
 */
object AnotherDSLExperiment {
    val tests = testCollection("collection name") {
        // these dependencies are resolved when the test runs
        val myDependency by beforeTest { MyDependency() }.afterTest { it.close() }
        // or setup/teardown (or setUp / tearDown)? before/after-test has the advantage that it species when it is called
        val myOtherDependency by beforeTest { MyOtherDependency(myDependency) }

        test("test name") {
            // now the dependencies are resolved
            myOtherDependency.doStuff()
            // ...
        }

        val thirdDependency by beforeTest { MyOtherDependency(myDependency) }
        test("another test") {
            // this test also has access to "thirdDependency"
            thirdDependency.doStuff()
            // ...
        }
        context("test group") {
            val contextDependency by beforeTest { ContextDependency(myOtherDependency) }
            test("test name") {
                // now the dependencies are resolved
                contextDependency.doStuff()
                // test body
            }

        }

    }
}

@Test
object InvestigatorTest {
    val tests = tests {
        it("can get a list of tests") {
            val result = Investigator().discover(testCollection("collection name") {
                test("test name") {
                }
                test("another test") {
                }
            })
            assertEquals(listOf(Test("test name"), Test("another test")), result)
        }
        it("can get a list of tests and contexts") {
            val result = Investigator().discover(AnotherDSLExperiment.tests)
            assertEquals(
                listOf(
                    Test("test name"),
                    Test("another test"),
                    TestGroup("test group", listOf(Test("test name")))
                ), result
            )
        }

    }
}

/*
 a test of a group of tests
 */
// TODO: add source info (file, line number)
sealed interface Node {
    val name: String

    data class Test(override val name: String) : Node
    data class TestGroup(override val name: String, val children: List<Node>) : Node
}


class Investigator {
    @Suppress("RedundantSuspendModifier", "RedundantSuppression") // IDEA BUG
    suspend fun discover(tests: TestCollection): List<Node> = discover(tests.function)

    private suspend fun discover(testFunction: TestFunction): List<Node> {
        val nodes = mutableListOf<Node>()

        class DiscoveringTestDSL : TestDSL {
            override fun <T> beforeTest(function: () -> T): Dependency<T> {
                return object : Dependency<T> {
                    override fun getValue(owner: Any?, property: KProperty<*>): T {
                        throw FailGoodException("dependencies should not be read during discovery")
                    }

                    override fun afterTest(function: suspend (T) -> Unit): Dependency<T> = this
                }
            }

            override suspend fun test(testName: String, function: () -> Unit) {
                nodes.add(Test(testName))

            }

            override suspend fun context(contextName: String, function: TestFunction) {
                nodes.add(TestGroup(contextName, discover(function)))
            }

        }

        testFunction(DiscoveringTestDSL())
        return nodes
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
    suspend fun close() {
        delay(0)
    }
}

interface TestDSL {
    fun <SubjectType> beforeTest(function: () -> SubjectType): Dependency<SubjectType>
    suspend fun test(testName: String, function: () -> Unit)
    suspend fun context(contextName: String, function: TestFunction)
}

interface Dependency<T> {
    operator fun getValue(owner: Any?, property: KProperty<*>): T
    fun afterTest(function: suspend (T) -> Unit): Dependency<T>
}

typealias TestFunction = suspend TestDSL.() -> Unit

private fun testCollection(name: String, function: TestFunction) = TestCollection(name, function)

data class TestCollection(val name: String, val function: TestFunction)
