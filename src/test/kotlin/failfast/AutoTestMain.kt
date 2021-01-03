package failfast

import failfast.internal.ContextExecutorTest

fun main() {
    autoTest(anyTestClass = ContextExecutorTest::class)
}
