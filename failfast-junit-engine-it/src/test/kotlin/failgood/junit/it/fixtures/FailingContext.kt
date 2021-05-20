package failgood.junit.it.fixtures

import failgood.FailFast
import failgood.describe
import org.junit.platform.commons.annotation.Testable

fun main() {
    FailFast.runTest()
}
@Testable
// to reproduce https://github.com/christophsturm/failgood/issues/10 ( manually :(( )
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
