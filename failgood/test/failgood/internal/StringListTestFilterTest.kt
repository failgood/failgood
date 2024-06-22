package failgood.internal

import failgood.Test
import failgood.TestCollection
import failgood.testCollection
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Test
class StringListTestFilterTest {
    val tests =
        testCollection(StringListTestFilter::class) {
            val f = StringListTestFilter(listOf("path", "to", "context"))
            it("executes a path that leads to a context") {
                expectThat(f.shouldRun(ContextPath.fromList("path", "to"))).isTrue()
            }
            it("executes a parent of the context") {
                expectThat(f.shouldRun(ContextPath.fromList("path", "to", "context", "test")))
                    .isTrue()
            }
            it("does not execute a different path") {
                expectThat(
                    f.shouldRun(ContextPath.fromList("path", "to", "some", "other", "context"))
                )
                    .isFalse()
            }
            it("executes a root context when the path fits") {
                expectThat(f.shouldRun(TestCollection("path") {})).isTrue()
            }
            it("does not execute a root context when the path is different") {
                expectThat(f.shouldRun(TestCollection("not path") {})).isFalse()
            }
        }
}
