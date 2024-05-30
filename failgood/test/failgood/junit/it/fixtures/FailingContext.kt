package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class FailingContext {
    val tests =
        testsAbout("root") {
            describe("context") { test("test") {} }
            describe("failing context") { throw RuntimeException() }
        }
}
