package failfast.examples

import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan

object FailFastTest {
    val context = describe("The test runner") {
        it("supports describe/it syntax") { expectThat(true).isEqualTo(true) }
        describe("nested contexts") {
            it("can contain tests too") { expectThat(true).isEqualTo(true) }

            describe("disabled/pending tests") {
                itWill("itWill can be used to mark pending tests") {}
                itWill("for pending tests the test body is optional")
            }
            context("context/test syntax is also supported") {
                test(
                    "I prefer describe/it but if there is no subject to describe I use " +
                            "context/test"
                ) {}
            }

            context("dynamic tests") {
                (1 until 5).forEach { contextNr ->
                    context("dynamic context #$contextNr") {
                        (1 until 5).forEach { testNr ->
                            test("test #$testNr") {
                                expectThat(testNr).isLessThan(10)
                                expectThat(contextNr).isLessThan(10)
                            }
                        }
                    }
                }
            }
        }
    }
}
