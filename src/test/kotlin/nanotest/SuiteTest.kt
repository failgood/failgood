package nanotest

import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

object SuiteTest {
    val context = Context("SuiteTest") {
        val context = Context("one failing one passing test") {
            test("firstTest") {
                expectThat(true).isTrue()
            }
            test("failing test") {
                expectThat(true).isFalse()
            }
        }
        val tenContexts = (1 until 10).map { context.copy(name = "context $it") }
        test("Suite has Contexts") {
            val suite = Suite(tenContexts)
            expectThat(suite.contexts).isEqualTo(tenContexts)
        }

    }
}
