package failgood.junit.legacy

import failgood.Test
import failgood.testsAbout

@Test
object LegacyJUnitTestEngineTest {
    val tests =
        testsAbout(LegacyJUnitTestEngine::class) {
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
