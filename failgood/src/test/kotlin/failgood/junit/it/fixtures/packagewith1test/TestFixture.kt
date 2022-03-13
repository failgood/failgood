package failgood.junit.it.fixtures.packagewith1test

import failgood.describe

object TestFixture {
    val CONTEXT_NAME = "the test fixture"
    val context = describe(CONTEXT_NAME) {}
}
