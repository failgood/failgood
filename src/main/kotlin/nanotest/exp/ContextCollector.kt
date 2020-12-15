package nanotest.exp

import nanotest.Context
import nanotest.ContextDSL
import nanotest.ContextLambda

internal class ContextCollector(private val context: Context) {

    val tests = mutableListOf<TestDescriptor>()
    val contexts = mutableListOf<List<String>>()

    inner class ContextVisitor(private val parentContexts: List<String>) : ContextDSL {
        override fun test(name: String, function: () -> Unit) {
            tests.add(TestDescriptor(parentContexts, name))
        }

        override fun xtest(ignoredTestName: String, function: () -> Unit) {
        }

        override fun context(name: String, function: ContextLambda) {
            val context = parentContexts + name
            contexts.add(context)
            ContextVisitor(context).function()
        }

        override fun <T> autoClose(wrapped: T, closeFunction: (T) -> Unit) = wrapped
    }

    fun execute(): ContextInfo {
        ContextVisitor(listOf()).(context.function)()
        return ContextInfo(context, contexts, tests)
    }
}

data class ContextInfo(val rootContext: Context, val contexts: List<List<String>>, val tests: List<TestDescriptor>)
data class TestDescriptor(val parentContexts: List<String>, val name: String)
