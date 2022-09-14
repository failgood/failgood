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
        val list = listOf(A, B, C)
        describe("containsExactlyInAnyOrder") {
            it("works with list") {
                assert(list.containsExactlyInAnyOrder(listOf(A, B, C)))
                assert(list.containsExactlyInAnyOrder(listOf(A, C, B)))
                assert(!list.containsExactlyInAnyOrder(listOf(A, B)))
            }
            it("works with vararg") {
                assert(list.containsExactlyInAnyOrder(A, B, C))
                assert(list.containsExactlyInAnyOrder(A, C, B))
                assert(!list.containsExactlyInAnyOrder(A, B))
            }
        }
        describe("endsWith") {
            it("works with list") {
                assert(list.endsWith(listOf(C)))
                assert(list.endsWith(listOf(B, C)))
                assert(!list.endsWith(listOf(C, B)))
            }
            it("works with vararg") {
                assert(list.endsWith(C))
                assert(list.endsWith(B, C))
                assert(!list.endsWith(C, B))
            }
        }
    }
}
