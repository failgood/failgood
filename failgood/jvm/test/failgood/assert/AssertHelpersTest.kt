package failgood.assert

import failgood.Test
import failgood.assert.Letters.*
import failgood.describe

enum class Letters {
    A, B, C
}
@Test
object AssertHelpersTest {
    val tests = describe("AssertHelpers") {
        describe("containsExactlyInAnyOrder") {
            it("returns true when the content is the same") {
                assert(listOf(A, B, C).containsExactlyInAnyOrder(listOf(A, B, C)))
                assert(listOf(A, B, C).containsExactlyInAnyOrder(listOf(A, C, B)))
            }
            it("returns false when the content is different") {
                assert(!listOf(A, B, C).containsExactlyInAnyOrder(listOf(A, B)))
            }
        }
    }
}
