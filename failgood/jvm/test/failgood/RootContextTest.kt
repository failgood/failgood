package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class RootContextTest {
    private val ste = Throwable().stackTrace.first()!!
    private val otherContext = describe("context fixture") {}
    val context: RootContext =
        describe(RootContext::class) {
            val testContext = otherContext
            it("knows its className and line number") {
                expectThat(testContext.sourceInfo) {
                    get { lineNumber }.isEqualTo(ste.lineNumber + 1)
                    get { className }.isEqualTo("failgood.RootContextTest")
                }
            }
        }
}
