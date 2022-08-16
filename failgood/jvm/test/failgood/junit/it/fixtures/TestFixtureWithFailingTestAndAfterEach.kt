package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import java.lang.RuntimeException
import kotlin.test.DefaultAsserter.fail

@TestFixture
object TestFixtureWithFailingTestAndAfterEach {
    val context = describe("root context") {
        it("the test name") {
            fail("fail")
        }
        afterEach {
            throw RuntimeException()
        }
    }
}
