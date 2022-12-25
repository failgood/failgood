package failgood.exp.suite

import failgood.RootContext
import failgood.internal.execution.context.ContextExecutorTest

object SuitePlayground {
    val suite = TestSuite().apply {
        prepare {
            // init some test dependencies that we want to start asap because they need time to become read
        }
        runTestsInPackage("failgood")
        // or
        runTest(ContextExecutorTest.context)
    }

}

class TestSuite {
    fun runTestsInPackage(s: String) {
    }

    fun prepare(function: () -> Unit) {
        TODO("Not yet implemented")
    }

    fun runTest(context: RootContext) {
        TODO("Not yet implemented")
    }

}
