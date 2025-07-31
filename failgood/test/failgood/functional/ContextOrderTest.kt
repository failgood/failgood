package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.testCollection

@Test
class ContextOrderTest {
    val tests =
        testCollection("Root Context Order") {
            it("is determined by the order field low to high") {
                val events = mutableListOf<String>()
                val result =
                    Suite(
                            listOf(
                                testCollection("context 1", order = 1) {
                                    events.add("context 1")
                                    test("test") {}
                                },
                                testCollection("context 2", order = 0) {
                                    events.add("context 2")
                                    test("test") {}
                                }))
                        .run(parallelism = 1, silent = true)
                assert(result.allOk)
                assert(events == listOf("context 2", "context 1"))
            }
        }
}
