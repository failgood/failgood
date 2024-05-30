package failgood.junit.it.fixtures.packagewith1test

import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
object SimpleTestFixture {
    const val CONTEXT_NAME = "the test fixture"
    val tests = testsAbout(CONTEXT_NAME) {}
}
