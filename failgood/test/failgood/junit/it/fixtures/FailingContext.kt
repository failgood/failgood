package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

// to reproduce https://github.com/failgood/failgood/issues/10 ( manually :(( )
@TestFixture
class FailingContext {
    val context =
        tests("root") {
            describe("failing context") { throw RuntimeException() }
            describe("context") { test("test") {} }
        }
}
