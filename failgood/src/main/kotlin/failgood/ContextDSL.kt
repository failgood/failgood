package failgood

interface ResourcesDSL {
    /**
     * asynchronously create a dependency. This is great for blocking dependencies, like a docker container.
     * The creator lambda runs on the IO dispatcher to make a cpu thread free for a test
     */
    suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit = {}): TestDependency<T>

    /**
     * create a test dependency that should be closed after the test run.
     * use this instead of beforeEach/afterEach
     */
    fun <T : AutoCloseable> autoClose(wrapped: T): T

    /**
     * create a test dependency that should be closed after the test run.
     * use this instead of beforeEach/afterEach
     */
    fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T

    /**
     * Register a callback that will run after each test. use [autoClose] instead if you can.
     */
    fun afterEach(function: suspend TestDSL.(TestResult) -> Unit)
}

@FailGoodDSL
interface ContextDSL<GivenType> : ResourcesDSL {
    /**
     * define a test context that describes a subject.
     */
    suspend fun describe(name: String, tags: Set<String> = setOf(), function: ContextLambda)

    /**
     * define a test that describes one aspect of a subject.
     */
    suspend fun it(behaviorDescription: String, tags: Set<String> = setOf(), function: GivenTestLambda<GivenType>)

    /**
     * define a test context. if possible prefer [describe] with a description of behavior.
     */
    suspend fun context(name: String, tags: Set<String> = setOf(), function: ContextLambda)

    /**
     * define a test. [it] is probably better suited.
     */
    suspend fun test(name: String, tags: Set<String> = setOf(), function: GivenTestLambda<GivenType>)

    /**
     * define a context with a given block. the given block will be called for every test and passed as argument
     */
    suspend fun <ContextDependency> context(
        contextName: String,
        tags: Set<String> = setOf(),
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    )

    /**
     * define a context that describes a subject with a given block.
     * the given block will be called for every test and passed as argument
     */
    suspend fun <ContextDependency> describe(
        contextName: String,
        tags: Set<String> = setOf(),
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    )

    /**
     * define a pending test.
     */
    suspend fun pending(behaviorDescription: String, function: TestLambda = {})

    /**
     * Register a callback to be called after all tests have completed
     */
    fun afterSuite(function: suspend () -> Unit)
}
