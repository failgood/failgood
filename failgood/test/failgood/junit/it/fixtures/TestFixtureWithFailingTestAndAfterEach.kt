package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testCollection
import kotlin.test.DefaultAsserter.fail

@TestFixture
object TestFixtureWithFailingTestAndAfterEach {
    val tests =
        testCollection("root context") {
            it("the test name") { fail("fail") }
            afterEach { throw RuntimeException() }
        }
}
