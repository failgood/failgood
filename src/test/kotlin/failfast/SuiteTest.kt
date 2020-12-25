package failfast

import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

object SuiteTest {
    val context = context {
        val context = RootContext("one failing one passing test") {
            test("firstTest") {
                expectThat(true).isTrue()
            }
            test("failing test") {
                expectThat(true).isFalse()
            }
        }
        val contexts = (1 until 2).map { context.copy(name = "context $it") }
        test("Empty Suite fails") {
            expectThrows<RuntimeException> {
                Suite(listOf())
            }
        }
        test("Suite is stateless") {
            val suite = Suite.fromContexts(contexts)
            // both run calls have to be on the same line to make the exception stacktrace equal
            val (firstRun, secondRun) = listOf(suite.run(), suite.run())
            expectThat(firstRun).isEqualTo(secondRun)
        }
        test("Suite {} creates a root context") {
            expectThat(Suite {
                test("test") {}
            }.rootContexts.single().getContext().name).isEqualTo("root")
        }

    }
}
