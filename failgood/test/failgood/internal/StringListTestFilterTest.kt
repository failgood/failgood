package failgood.internal

import failgood.Test
import failgood.TestCollection
import failgood.testCollection

@Test
class StringListTestFilterTest {
    val tests =
        testCollection(StringListTestFilter::class) {
            val f = StringListTestFilter(listOf("path", "to", "context"))
            it("executes a path that leads to a context") {
                assert(f.shouldRun(ContextPath.fromList("path", "to")))
            }
            it("executes a parent of the context") {
                assert(f.shouldRun(ContextPath.fromList("path", "to", "context", "test")))
            }
            it("does not execute a different path") {
                assert(!f.shouldRun(ContextPath.fromList("path", "to", "some", "other", "context")))
            }
            it("executes a root context when the path fits") {
                assert(f.shouldRun(TestCollection("path") {}))
            }
            it("does not execute a root context when the path is different") {
                assert(!f.shouldRun(TestCollection("not path") {}))
            }
        }
}
