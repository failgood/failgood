package failgood.dsl

import failgood.SuspendAutoCloseable
import failgood.TestDependency
import failgood.TestResult

/**
 * Lifecycle functions for resources that are used by tests. This is a separate interface because it
 * is available in the ContextDSL and in the TestDSL
 */
interface ResourcesDSL {
    /**
     * Create a test dependency that should be closed after the test runs. use this instead of
     * beforeEach/afterEach In contexts with isolation the AutoClosable is closed after the test, in
     * contexts without isolation after the suite
     */
    fun <T : AutoCloseable> autoClose(autoCloseable: T): T

    fun <T : SuspendAutoCloseable> autoClose(autoCloseable: T): T

    /**
     * Create a test dependency that should be closed after the test run. use this instead of
     * beforeEach/afterEach In contexts with isolation the close function runs after the test. In
     * contexts without isolation it runs after the suite
     */
    fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T
}

/** Resource management functions that are only available in the context and not in a test */
interface ContextOnlyResourceDSL {
    /**
     * Register a callback that will run after each test. use [ResourcesDSL.autoClose] instead if
     * you can. This will be called after each test even in contexts that have no isolation
     */
    fun afterEach(function: suspend TestDSL.(TestResult) -> Unit)
    /**
     * Asynchronously create a dependency. This is great for blocking dependencies, like a docker
     * container. The creator lambda runs on the IO dispatcher to make a cpu thread free for a test.
     * the close function works just like in [ResourcesDSL.autoClose]
     */
    suspend fun <T> dependency(
        creator: suspend () -> T,
        closer: suspend (T) -> Unit = {}
    ): TestDependency<T>
}
