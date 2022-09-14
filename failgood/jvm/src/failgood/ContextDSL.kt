package failgood

/**
 * Lifecycle functions for resources that are used by tests.
 * This is a separate interface because it is available in the ContextDSL and in the TestDSL
 */
interface ResourcesDSL {
    /**
     * Create a test dependency that should be closed after the test runs.
     * use this instead of beforeEach/afterEach
     * In contexts with isolation the AutoClosable is closed after the test,
     * in contexts without isolation after the suite
     */
    fun <T : AutoCloseable> autoClose(wrapped: T): T

    /**
     * Create a test dependency that should be closed after the test run.
     * use this instead of beforeEach/afterEach
     * In contexts with isolation the close function runs after the test.
     * In contexts without isolation it runs after the suite
     */
    fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T

    /**
     * Register a callback that will run after each test. use [autoClose] instead if you can.
     * This will be called after each test even in contexts that have no isolation
     */
    fun afterEach(function: suspend TestDSL.(TestResult) -> Unit)

    /**
     * Asynchronously create a dependency. This is great for blocking dependencies, like a docker container.
     * The creator lambda runs on the IO dispatcher to make a cpu thread free for a test.
     * the close function works just like in [autoClose]
     */
    suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit = {}): TestDependency<T>
}

/**
 * This is used to define test contexts and tests.
 * It is recommended to use [describe]/[it]-syntax, but if you really have to you can also use [context] to define a context
 * and [test] to define a test
 */
@FailGoodDSL
interface ContextDSL<GivenType> : ResourcesDSL {
    /**
     * Define a context that describes a subject with a given block.
     * set [isolation] false to turn off test isolation for this context.
     * the given block will be called for every test and passed as argument, even if the context has isolation off
     */
    suspend fun <ContextDependency> describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    )

    /**
     * Define a test context that describes a subject.
     * this is a helper function for contexts without a given block
     */
    suspend fun describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        function: ContextLambda
    )

    /**
     * Define an ignored test.
     */
    suspend fun ignore(name: String, function: TestLambda<GivenType> = {})

    /**
     * Register a callback to be called after all tests have completed
     */
    fun afterSuite(function: suspend () -> Unit)

    /**
     * Define a test that describes one aspect of a subject.
     */
    suspend fun it(name: String, tags: Set<String> = setOf(), function: TestLambda<GivenType>)

    // Support for context/test. This will maybe be removed before 1.0, unless somebody really loves it.
    // another option would be to move it to a separate sub interface.
    /**
     * Define a context with a given block. The given block will be called for every test and passed as argument,
     * even if isolation is turned off.
     */
    suspend fun <ContextDependency> context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    ) = describe(name, tags, isolation, given, contextLambda)

    /**
     * Define a test context. Prefer [describe] with a description of behavior.
     */
    suspend fun context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        function: ContextLambda
    ) = describe(name, tags, isolation, function)

    /**
     * Define a test. Prefer [it]
     */
    suspend fun test(name: String, tags: Set<String> = setOf(), function: TestLambda<GivenType>) =
        it(name, tags, function)
}
