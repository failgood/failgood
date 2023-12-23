package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout

// to reproduce https://github.com/failgood/failgood/issues/10 ( manually :(( )
@TestFixture
class FailingContext {
    val context =
        testsAbout("root") {
            describe("failing context") { throw RuntimeException() }
            describe("context") { test("test") {} }
        }
}
