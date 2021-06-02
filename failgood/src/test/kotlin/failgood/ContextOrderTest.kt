package failgood

import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isTrue

@Testable
class ContextOrderTest {
    val context = describe("Root Context Order", disabled = true) {
        it("is determined by the order field low to high") {
            val events = mutableListOf<String>()
            expectThat(
                Suite.fromContexts(
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
                    )
                ).run().allOk
            ).isTrue()
            expectThat(events).containsExactly("context 2", "context 1")
        }
    }
}
