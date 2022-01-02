package failgood

import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Test
class SuiteTest {
    val context =
        describe(Suite::class) {
            test("Empty Suite fails") { expectThrows<RuntimeException> { Suite(listOf<ContextProvider>()) } }
            test("Suite {} creates a root context") {
                expectThat(Suite { test("test") {} }.contextProviders.single().getContexts().single().name)
                    .isEqualTo("root")
            }
            test("runSingleTest works") {
                expectThat(
                    Suite {
                        test("test") {
                            println("")
                        }
                    }.runSingle("root > test")
                ).isA<Success>()
            }
        }
}
