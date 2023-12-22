package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests
import java.lang.RuntimeException
import kotlin.test.DefaultAsserter.fail

@TestFixture
object TestFixtureWithFailingTestAndAfterEach {
    val context =
        tests("root context") {
            it("the test name") { fail("fail") }
            afterEach { throw RuntimeException() }
        }
}
