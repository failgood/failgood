package failgood.examples

import failgood.Test
import failgood.describe

@Test
class FailGoodDSLExample {
    val context = describe("The test runner") {
        it("supports describe/it syntax") { assert(true) }
        describe("nested contexts") {
            it("can contain tests too") { assert(true) }

            describe("disabled/pending tests") {
                ignore("ignore can be used to disable  tests that are unfinished") {}
                ignore("for ignore tests the test body is optional," +
                        " you can use it as reminder of tests that you want to write")
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
