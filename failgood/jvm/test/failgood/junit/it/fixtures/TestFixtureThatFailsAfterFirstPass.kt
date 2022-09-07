package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture

@TestFixture
class TestFixtureThatFailsAfterFirstPass {
    var invocations = 0
    val contextThatFailsAfter1 = rootContext(1)
    val contextThatFailsAfter2 = rootContext(2)
    val contextThatFailsAfter3 = rootContext(3) // let's test another version because its so easy

    private fun rootContext(failAfter: Int) = describe("a test context that fails in its third pass") {
            // not sure why it needs 3 invocations to trigger this bug.
            if (invocations++ == failAfter)
                throw RuntimeException()
            it("test 1") {
            }
            describe("sub context") {
                it("test 2") {
                }
                describe("another subcontext") {
                    it("test 3") {
                    }
                    describe("another subcontext") {
                        it("test 3") {
                        }
                    }
                }
            }
        }
}
