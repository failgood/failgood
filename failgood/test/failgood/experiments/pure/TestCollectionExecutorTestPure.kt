package failgood.experiments.pure

import failgood.ExecutionListener
import failgood.Ignored
import failgood.NullExecutionListener
import failgood.TestCollection
import failgood.internal.ContextInfo
import failgood.internal.ContextResult
import failgood.internal.execution.TestCollectionExecutor
import kotlin.test.assertNotNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * here I am trying to port one of the most complex tests to the pure given dsl. nothing to see here
 * for now.
 */
object TestCollectionExecutorTestPure {
    class Given {
        val assertionError: java.lang.AssertionError = AssertionError("failed")
        val context: TestCollection<Unit> =
            TestCollection("root context") {
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

        suspend fun execute(
            tag: String? = null,
            listener: ExecutionListener = NullExecutionListener
        ): ContextResult {
            return coroutineScope {
                TestCollectionExecutor(context, this, runOnlyTag = tag, listener = listener).execute()
            }
        }
    }

    val test =
        root(
            "contextExecutorTest port",
            given = { Given() },
            context(
                "executing all the tests",
                given = { assertNotNull(given.execute() as? ContextInfo) }
            )
        )
}
