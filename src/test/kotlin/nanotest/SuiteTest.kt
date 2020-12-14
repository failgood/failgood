package nanotest

import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

object SuiteTest {
    val context = Context {
        val context = Context("one failing one passing test") {
            test("firstTest") {
                expectThat(true).isTrue()
            }
            test("failing test") {
                expectThat(true).isFalse()
            }
        }
        val contexts = (1 until 2).map { context.copy(name = "context $it") }
        test("Suite has Contexts") {
            val suite = Suite(contexts)
            expectThat(suite.contexts).isEqualTo(contexts)
        }
        test("Empty Suite fails") {
            expectThrows<RuntimeException> {
                Suite(listOf())
            }
        }
        test("Suite is stateless") {
            val suite = Suite(contexts)
            // both run calls have to be on the same line to make the exception stacktrace equal
            val (firstRun, secondRun) = listOf(suite.run(), suite.run())
            expectThat(firstRun).isEqualTo(secondRun)
        }
        test("Suite {} creates a root context") {
            expectThat(Suite {
                test("test") {}
            }.contexts.single().name).isEqualTo("root")
        }

    }
}
