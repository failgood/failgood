package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
// to reproduce https://github.com/failgood/failgood/issues/10 ( manually :(( )
class FailingContext {
    val context = describe("root") {
        describe("failing context") {
            throw RuntimeException()
        }
        describe("context") {
            test("test") {}
        }
    }
}
