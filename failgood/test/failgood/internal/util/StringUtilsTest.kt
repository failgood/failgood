package failgood.internal.util

import failgood.Test
import failgood.describe
import failgood.tests

@Test
object StringUtilsTest {
    val tests =
        tests("StringUtils") {
            describe("pluralize") {
                it("pluralizes") { assert(pluralize(2, "item") == "2 items") }
                it("does not pluralize") { assert(pluralize(1, "item") == "1 item") }
            }
        }
}
