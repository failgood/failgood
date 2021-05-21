package failgood.docs

import failgood.describe
import org.junit.platform.commons.annotation.Testable

@Testable
class TestContextExampleTest {
    val context = describe("examples for the test context dsl") {
        it("published println calls in the junit runner") {
            // this will output
            // timestamp = 2021-04-12T17:45:25.233319, stdout = this is my debug output
            // in the junit test runner.
            println("this is my debug output")
        }

    }

}
