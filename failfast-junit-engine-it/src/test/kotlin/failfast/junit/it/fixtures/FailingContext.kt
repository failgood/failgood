package failfast.junit.it.fixtures

import failfast.describe
import org.junit.platform.commons.annotation.Testable

@Testable
// to reproduce https://github.com/christophsturm/failfast/issues/10 ( manually :(( )
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
