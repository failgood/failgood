package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
class TestFixtureThatFailsAfterFirstPass {
    var invocations = 0
    val context = describe("a test context that fails in its third pass") {
        // not sure why it needs 3 invocations to trigger this bug.
        if (invocations++ == 2)
            throw RuntimeException()
        it("test 1") {
        }
        describe("sub context") {
            it("test 2") {
            }
            describe("another subcontext") {
                it("test 3") {
                }
            }
        }
    }
}
