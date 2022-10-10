package failgood.assert

import failgood.Test
import failgood.describe

@Test
object AssertHelpersTest {
    val tests = describe("AssertHelpers") {
        describe("containsExactlyInAnyOrder") {
            describe("simple") {
                it("returns true when the elements are the same") {
                    assert(listOf("a", "b").containsExactlyInAnyOrder("b", "a"))
                }
                it("returns true false the elements are not the same") {
                    assert(!listOf("a", "b", "c").containsExactlyInAnyOrder("b", "a"))
                }
            }
            describe("with a class that does not implement comparable") {
                class C(val v: String) {
                    override fun toString(): String {
                        return "C(v='$v')"
                    }
                }
                // use entries that do not implement comparable
                val a = C("A")
                val b = C("B")
                val c = C("C")
                val list = listOf(a, b, c)
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
        }
        describe("endsWith") {
            it("works with list") {
                assert(listOf("a", "b", "c").endsWith(listOf("c")))
                assert(listOf("a", "b", "c").endsWith(listOf("b", "c")))
                assert(listOf("a", "b", "c").endsWith(listOf("a", "b", "c")))
            }
            it("works with vararg") {
                assert(listOf("a", "b", "c").endsWith("b", "c"))
            }
        }
    }
}
