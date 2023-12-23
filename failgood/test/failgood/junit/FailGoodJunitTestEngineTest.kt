package failgood.junit

import failgood.Test
import failgood.describe
import failgood.testsAbout

@Test
object FailGoodJunitTestEngineTest {
    val tests =
        testsAbout(FailGoodJunitTestEngine::class) {
            it("parses a filter string") {
                val filters =
                    parseFilterString(
                        "The ContextExecutor > with a valid root context" +
                                " > executing all the tests ✔ returns deferred test results"
                    )
                assert(
                    filters ==
                            listOf(
                                "The ContextExecutor",
                                "with a valid root context",
                                "executing all the tests",
                                "returns deferred test results"
                            )
                )
            }
        }
}
