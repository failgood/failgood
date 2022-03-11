package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe

@Test
class TestWithNestedContextsTest {
    companion object {
        const val CHILD_CONTEXT_2_NAME = "child context level 2"
        const val ROOT_CONTEXT_NAME = "fixture-root-context"
        const val CHILD_CONTEXT_1_NAME = "child context level 1"
        const val TEST_NAME = "finally the test"
        const val TEST2_NAME = "another test"
    }

    val context = describe(ROOT_CONTEXT_NAME) {
        describe(CHILD_CONTEXT_1_NAME) {
            describe(CHILD_CONTEXT_2_NAME) {
                test(TEST_NAME) {}
                test(TEST2_NAME) {}
            }
        }
    }
}
