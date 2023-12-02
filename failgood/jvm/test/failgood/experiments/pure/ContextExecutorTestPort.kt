package failgood.experiments.pure

import failgood.Ignored
import failgood.RootContext
import kotlinx.coroutines.delay

/**
 * here I am trying to port one of the most complex tests to the pure given dsl. nothing to see here
 * for now.
 */
object ContextExecutorTestPort {
    class Given {
        val assertionError: java.lang.AssertionError = AssertionError("failed")
        val context: RootContext =
            RootContext("root context") {
                test("test 1") { delay(1) }
                test("test 2") { delay(1) }
                test("ignored test", ignored = Ignored.Because("testing")) {}
                test("failed test") { throw assertionError }
                context("context 1") {
                    // comment to make sure that context1 and context2 are not on the same
                    // line
                    context("context 2") { test("test 3") { delay(1) } }
                }
                context("context 3") { test("test 4") { delay(1) } }
            }
    }

    val test =
        context(
            "contextExecutorTest port",
            given = { Given() },
            context("executing all the tests", given = {})
        )
}
