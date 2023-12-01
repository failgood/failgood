package failgood.experiments.pure

import failgood.Ignored
import failgood.RootContext
import kotlinx.coroutines.delay

object ContextExecutorTestPort {
    data class Given(
        val assertionError: java.lang.AssertionError? = null,
        var context: RootContext? = null
    )

    val test =
        context(
            "contextExecutorTest port",
            given = {
                val given = Given()
                given.context =
                    RootContext("root context") {
                        test("test 1") { delay(1) }
                        test("test 2") { delay(1) }
                        test("ignored test", ignored = Ignored.Because("testing")) {}
                        test("failed test") {
                            given.assertionError = AssertionError("failed")
                            throw given.assertionError!!
                        }
                        context("context 1") {
                            // comment to make sure that context1 and context2 are not on the same
                            // line
                            context("context 2") { test("test 3") { delay(1) } }
                        }
                        context("context 3") { test("test 4") { delay(1) } }
                    }
                given
            },
            context("executing all the tests", given = {})
        )
}
