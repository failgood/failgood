package failgood.assert

import failgood.Test
import failgood.describe

@Test
object AssertHelpersTest {
    val tests = describe("AssertHelpers") {
        // use entries that do not implement comparable
        val a = String::class
        val b = Int::class
        val c = Long::class
        val list = listOf(a, b, c)
        describe("containsExactlyInAnyOrder") {
            it("works with list") {
                assert(list.containsExactlyInAnyOrder(listOf(a, b, c)))
                assert(list.containsExactlyInAnyOrder(listOf(a, c, b)))
                assert(!list.containsExactlyInAnyOrder(listOf(a, b)))
            }
            it("works with vararg") {
                assert(list.containsExactlyInAnyOrder(a, b, c))
                assert(list.containsExactlyInAnyOrder(a, c, b))
                assert(!list.containsExactlyInAnyOrder(a, b))
            }
        }
        describe("endsWith") {
            it("works with list") {
                assert(list.endsWith(listOf(c)))
                assert(list.endsWith(listOf(b, c)))
                assert(!list.endsWith(listOf(c, b)))
            }
            it("works with vararg") {
                assert(list.endsWith(c))
                assert(list.endsWith(b, c))
                assert(!list.endsWith(c, b))
            }
        }
    }
}
