package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout
import java.lang.RuntimeException
import kotlin.test.DefaultAsserter.fail

@TestFixture
object TestFixtureWithFailingTestAndAfterEach {
    val tests =
        testsAbout("root context") {
            it("the test name") { fail("fail") }
            afterEach { throw RuntimeException() }
        }
}
