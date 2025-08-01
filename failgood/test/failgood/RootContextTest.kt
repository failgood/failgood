package failgood

@Test
class RootContextTest {
    private val ste = Throwable().stackTrace.first()!!
    private val otherContext = testCollection("context fixture") {}
    val context: TestCollection<Unit> = testCollection {
        val testContext = otherContext
        it("knows its className and line number") {
            assert(testContext.sourceInfo.lineNumber == ste.lineNumber + 1)
            assert(testContext.sourceInfo.className == "failgood.RootContextTest")
        }
    }
}
