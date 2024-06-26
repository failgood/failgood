package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class TestFixtureThatFailsAfterFirstPass {
    private var invocations = 0
    val tests =
        testCollection("a test context that fails after 2 passes") {
            // not sure why it needs 3 invocations to trigger this bug.
            if (invocations++ == 2) throw RuntimeException()
            it("test") {}
            describe("sub context") {
                it("test") {}
                describe("another subcontext") {
                    it("test 3") {}
                    describe("another subcontext") { it("test") {} }
                }
            }
        }
}
