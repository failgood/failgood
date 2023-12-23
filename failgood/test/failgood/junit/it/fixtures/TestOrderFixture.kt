package failgood.junit.it.fixtures

import failgood.dsl.ContextDSL
import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
class TestOrderFixture {
    val context =
        testsAbout("test order fixture") {
            fourTests()
            context("context 1") { fourTests() }
            context("context 2") { fourTests() }
            context("context 3") { fourTests() }
            context("context 4") { fourTests() }
        }

    private suspend fun ContextDSL<Unit>.fourTests() {
        test("test 1") {}
        test("test 2") {}
        test("test 3") {}
        test("test 4") {}
    }
}
