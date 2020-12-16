package nanotest

import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isA

object ContextExecutorTest {
    val context = Context {
        test("context executor test") {
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
            val result: List<TestResult> = ContextExecutor(ctx).execute()
            expectThat(events).containsExactly(
                "root context", "test 1",
                "root context", "test 2",
                "root context", "context 1", "context 2", "test 3"
            )
            expectThat(result).all { isA<Success>() }

        }
    }
}

class ContextExecutor(private val context: Context) {

    private val testResults = mutableListOf<TestResult>()
    val excecutedTests = mutableSetOf<TestDescriptor>()

    val contexts = mutableListOf<List<String>>()

    inner class ContextVisitor(private val parentContexts: List<String>) : ContextDSL {
        var ranATest = false
        var moreTestsLeft = false
        override fun test(name: String, function: () -> Unit) {
            val testDescriptor = TestDescriptor(parentContexts, name)
            if (excecutedTests.contains(testDescriptor)) {
                return
            } else if (!ranATest) {
                excecutedTests.add(testDescriptor)
                val testResult = try {
                    function()
                    Success(testDescriptor)
                } catch (e: AssertionError) {
                    Failed(testDescriptor, e)
                }
                testResults.add(testResult)
                ranATest = true
            } else {
                moreTestsLeft = true
            }
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
            val testDescriptor = TestDescriptor(parentContexts, ignoredTestName)
            testResults.add(Ignored(testDescriptor))
        }

        override fun context(name: String, function: ContextLambda) {
            // if we already ran a test in this context we don't need to visit the child context now
            if (ranATest) {
                moreTestsLeft = true // but we need to run the root context again to visit this child context
                return
            }
            val context = parentContexts + name
            contexts.add(context)
            val visitor = ContextVisitor(context)
            visitor.function()
            if (visitor.moreTestsLeft)
                moreTestsLeft = true
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit) = wrapped
    }

    fun execute(): List<TestResult> {
        val function = context.function
        while (true) {
            val visitor = ContextVisitor(listOf())
            visitor.function()
            if (!visitor.moreTestsLeft)
                break
        }
        return testResults
    }
}
