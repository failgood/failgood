package failfast.docs

import failfast.ContextDSL
import failfast.Suite
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

fun main() {
    Suite(ContractsTest.context).run().check()
}

object ContractsTest {
    val context = describe("Contracts") {
        describe("ArrayList") {
            behavesAsMutableCollection(ArrayList<String>())
        }
        describe("LinkedList") {
            behavesAsMutableCollection(LinkedList<String>())
        }
    }

    private suspend fun ContextDSL.behavesAsMutableCollection(fixture: MutableList<String>) {
        context("behaves as MutableCollection") {

            test("is empty when created") {
                expectThat(fixture.isEmpty())
            }

            test("can add") {
                fixture.add("item")
                expectThat(fixture.first()).isEqualTo("item")
            }
        }
    }
}
