package nanotest.exp

import nanotest.Context
import nanotest.ContextDSL
import nanotest.ContextLambda
import nanotest.Failed
import nanotest.Success
import nanotest.TestResult

internal data class TestContext2(val name: String, val function: ContextLambda) : ContextDSL {
    constructor(context: Context) : this(context.name, context.function)

    private val closeables = mutableListOf<AutoCloseable>()
    val testResults = mutableListOf<TestResult>()
    private val childContexts = mutableListOf<TestContext2>()

    fun allChildContexts(): List<TestContext2> = childContexts.flatMap { it.allChildContexts() } + childContexts
    override fun test(testName: String, function: () -> Unit) {
        try {
            function()
            testResults.add(Success(testName))
        } catch (e: AssertionError) {
            testResults.add(Failed(testName, e))
        }
    }

    @Suppress("UNUSED_PARAMETER", "unused")
    override fun xtest(ignoredTestName: String, function: () -> Unit) {
    }

    override fun context(name: String, function: ContextLambda) {
        val element = TestContext2(name, function).execute()
        childContexts.add(element)
    }

    override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit): T {
        closeables.add(AutoCloseable { closeFunction(wrapped) })
        return wrapped
    }

    fun execute(): TestContext2 {
        function()
        closeables.forEach { it.close() }
        return this
    }
}
