package failgood.examples

import failgood.Ignored
import failgood.Test
import failgood.describe

@Test
class FailGoodDSLExample {
    val context = describe("The Failgood DSL") {
        it("supports describe/it syntax") { assert(true) }
        describe("nested contexts") {
            it("can contain tests too") { assert(true) }

            describe("disabled/pending tests") {
                it(
                    "ignore can be used to disable tests that are unfinished",
                    ignored = Ignored.Because("This is just an Example")
                ) {}
                test("ignore works for tests too", ignored = Ignored.Because("This is just an Example")) {}
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
                                assert(testNr < 10)
                                assert(contextNr < 10)
                            }
                        }
                    }
                }
            }
        }
    }
}
