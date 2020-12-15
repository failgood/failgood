package nanotest

import nanotest.exp.ContextCollector.TestDescriptor
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isA

object TestExecutorTest {
    val context = Context {
        val events = mutableListOf<String>()
        val ctx = Context("root context") {
            events.add("root context")
            test("test 1") {
                events.add("test 1")
            }
            test("test 2") {
                events.add("test 2")
            }
            context("context 1") {
                events.add("context 1")

                context("context 2") {
                    events.add("context 2")
                    test("test 3") {
                        events.add("test 3")
                    }
                }
            }
        }
        val tests = listOf(
            TestDescriptor(listOf(), "test 1"),
            TestDescriptor(listOf(), "test 2"),
            TestDescriptor(listOf("context 1", "context 2"), "test 3")
        )
        val result: List<TestResult> = tests.map { TestExecutor(ctx, it).execute() }
        expectThat(events).containsExactly(
            "root context", "test 1",
            "root context", "test 2",
            "root context", "context 1", "context 2", "test 3"
        )
        expectThat(result).all { isA<Success>() }
    }
}

internal class TestExecutor(val context: Context, val test: TestDescriptor) {
    private val closeables = mutableListOf<AutoCloseable>()
    private var testResult: TestResult? = null
    fun execute(): TestResult {
        val dsl: ContextDSL = contextDSL(test.parentContexts)
        dsl.(context.function)()
        return testResult!!
    }

    inner class ContextFinder(val contexts: List<String>) : ContextDSL {
        override fun test(name: String, function: () -> Unit) {
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
        }

        override fun context(name: String, function: ContextLambda) {
            if (contexts.first() != name)
                return

            contextDSL(contexts.drop(1)).function()
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            closeables.add(AutoCloseable { closeFunction(wrapped) })
            return wrapped
        }

    }

    private fun contextDSL(parentContexts: List<String>) = if (parentContexts.isEmpty())
        TestFinder(test.name)
    else
        ContextFinder(parentContexts)

    inner class TestFinder(val name: String) : ContextDSL {
        override fun test(name: String, function: () -> Unit) {
            if (this.name == name)
                try {
                    function()
                    testResult = Success(name)
                } catch (e: AssertionError) {
                    testResult = Failed(name, e)
                }
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
        }

        override fun context(name: String, function: ContextLambda) {
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
            closeables.add(AutoCloseable { closeFunction(wrapped) })
            return wrapped
        }

    }
}
