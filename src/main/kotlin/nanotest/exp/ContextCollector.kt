package nanotest.exp

import nanotest.Context
import nanotest.ContextDSL
import nanotest.ContextLambda

internal class ContextCollector(private val context: Context) {
    data class TestDescriptor(val parentContexts: List<String>, val name: String)

    val tests = mutableListOf<TestDescriptor>()

    inner class ContextVisitor(private val parentContexts: List<String>) : ContextDSL {
        override fun test(name: String, function: () -> Unit) {
            tests.add(TestDescriptor(parentContexts, name))
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
        }

        override fun context(name: String, function: ContextLambda) {
            ContextVisitor(parentContexts + name).function()
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit) = wrapped
    }

    fun execute(): List<TestDescriptor> {
        ContextVisitor(listOf()).(context.function)()
        return tests
    }
}
