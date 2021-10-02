package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class RootContextTest {
    // declare those on top level to make it more realistic
    val ste1 = Throwable().stackTrace.first()
    val testContext = RootContext {}
    val ste2 = Throwable().stackTrace.first()
    val testContextCreatedByMethod = createRootContext()
    val context: RootContext = describe(RootContext::class) {
        it("knows its file and line number") {
            expectThat(testContext.stackTraceElement) {
                get { lineNumber }.isEqualTo(ste1.lineNumber + 1)
                get { className }.isEqualTo(ste1.className)
            }
        }
        it("knows file and line number even when created by a utility method") {
            expectThat(testContextCreatedByMethod.stackTraceElement) {
                get { lineNumber }.isEqualTo(ste2.lineNumber + 1)
                get { className }.isEqualTo(ste2.className)
            }
        }
    }

}

private fun createRootContext(): RootContext {
    Throwable().printStackTrace()
    return RootContext {}
}
