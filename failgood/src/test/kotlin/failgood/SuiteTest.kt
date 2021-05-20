package failgood

import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Testable
class SuiteTest {
    val context =
        describe(Suite::class) {
            val context =
                RootContext("one failing one passing test") {
                    test("firstTest") { expectThat(true).isTrue() }
                    test("failing test") { expectThat(true).isFalse() }
                }
            test("Empty Suite fails") { expectThrows<RuntimeException> { Suite(listOf()) } }
            pending("create reproducible output") {
                val contexts = (1 until 2).map { context.copy(name = "context $it") }
                // currently two test runs don't give the same result because the test duration is
                // variable
                val suite = Suite.fromContexts(contexts)
                // both run calls have to be on the same line to make the exception stacktrace equal
                val (firstRun, secondRun) = listOf(suite.run(), suite.run())
                expectThat(firstRun).isEqualTo(secondRun)
            }
            test("Suite {} creates a root context") {
                expectThat(Suite { test("test") {} }.contextProviders.single().getContexts().single().name)
                    .isEqualTo("root")
            }
            test("runSingleTest works") {
                expectThat(Suite {
                    test("test") {
                        println("blah")
                    }
                }.rs("root > test")).isA<Success>()
            }
        }
}
