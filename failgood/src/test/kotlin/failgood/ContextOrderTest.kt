package failgood

import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isTrue

@Test
class ContextOrderTest {
    val context = describe("Root Context Order") {
        it("is determined by the order field low to high") {
            val events = mutableListOf<String>()
            expectThat(
                listOf(
                    describe("context 1", order = 1) {
                        events.add("context 1")
                        test("test") {
                        }
                    },
                    describe("context 2", order = 0) {
                        events.add("context 2")
                        test("test") {
                        }
                    },
                ).toSuite().run(parallelism = 1, silent = true).allOk
            ).isTrue()
            expectThat(events).containsExactly("context 2", "context 1")
        }
    }
}
