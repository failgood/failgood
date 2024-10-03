import failgood.Ignored.Because
import failgood.TestCollection
import kotlinx.coroutines.delay
import kotlin.test.assertEquals

fun main() {
    val tests =
        TestCollection("root context") {
            test("test 1") { delay(1) }
            test("test 2") { delay(1) }
            test("ignored test", ignored = Because("testing")) {}
            test("failed test") {
                assertEquals(1,2)

            }
            context("context 1") {
                test("context 1 test") {}
                // comment to make sure that context1 and context2 are not on the same
                // line
                context("context 2") { test("test 3") { delay(1) } }
            }
            context("context 3") { test("test 4") { delay(1) } }
        }
}
