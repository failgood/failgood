package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
class FailingContext {
    val tests =
        testCollection("root") {
            describe("context") { test("test") {} }
            describe("failing context") { throw RuntimeException() }
        }
}
