package failgood.examples

import failgood.Test
import failgood.describe
import failgood.dsl.ContextDSL
import failgood.tests
import java.util.LinkedList
import strikt.api.expectThat
import strikt.assertions.isEqualTo

// a port of
// https://github.com/dmcg/minutest/blob/master/core/src/test/kotlin/dev/minutest/examples/ContractsExampleTests.kt
@Test
class ContractsTest {
    val context =
        tests("Contracts") {
            describe("ArrayList") { behavesAsMutableCollection(ArrayList()) }
            describe("LinkedList") { behavesAsMutableCollection(LinkedList()) }
        }

    private suspend fun ContextDSL<Unit>.behavesAsMutableCollection(fixture: MutableList<String>) {
        context("behaves as MutableCollection") {
            test("is empty when created") { expectThat(fixture.isEmpty()) }

            test("can add") {
                fixture.add("item")
                expectThat(fixture.first()).isEqualTo("item")
            }
        }
    }
}
