package failgood.junit.it.fixtures.packagewith1test

import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
object SimpleTestFixture {
    const val CONTEXT_NAME = "the test fixture"
    val context = tests(CONTEXT_NAME) {}
}
