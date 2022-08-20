package failgood

interface ResourcesDSL {
    /**
     * create a test dependency that should be closed after the test runs.
     * use this instead of beforeEach/afterEach
     * In contexts with isolation this runs after the test.
     * In contexts without isolation it runs after the context
     */
    fun <T : AutoCloseable> autoClose(wrapped: T): T

    /**
     * create a test dependency that should be closed after the test run.
     * use this instead of beforeEach/afterEach
     * In contexts with isolation this runs after the test.
     * In contexts without isolation it runs after the context
     */
    fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T

    /**
     * Register a callback that will run after each test. use [autoClose] instead if you can.
     * This will be called after each tests even in contexts that have no isolation
     */
    fun afterEach(function: suspend TestDSL.(TestResult) -> Unit)

    /**
     * asynchronously create a dependency. This is great for blocking dependencies, like a docker container.
     * The creator lambda runs on the IO dispatcher to make a cpu thread free for a test
     */
    suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit = {}): TestDependency<T>
}

@FailGoodDSL
interface ContextDSL<GivenType> : ResourcesDSL {
    /**
     * define a context that describes a subject with a given block.
     * set [isolation] to false to turn off test isolation for this context
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
     * define a test context that describes a subject.
     * this is a helper function for contexts without a given block
     */
    suspend fun describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        function: ContextLambda
    )

    /**
     * define an ignored test.
     */
    suspend fun ignore(name: String, function: TestLambda<GivenType> = {})

    /**
     * Register a callback to be called after all tests have completed
     */
    fun afterSuite(function: suspend () -> Unit)

    /**
     * define a test that describes one aspect of a subject.
     */
    suspend fun it(name: String, tags: Set<String> = setOf(), function: TestLambda<GivenType>)

    /**
     * define a test context. if possible prefer [describe] with a description of behavior.
     */
    suspend fun context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        function: ContextLambda
    )

    /**
     * define a test. [it] is probably better suited.
     */
    suspend fun test(name: String, tags: Set<String> = setOf(), function: TestLambda<GivenType>)

    /**
     * define a context with a given block. the given block will be called for every test and passed as argument,
     * even if isolation is turned off.
     */
    suspend fun <ContextDependency> context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    )
}
