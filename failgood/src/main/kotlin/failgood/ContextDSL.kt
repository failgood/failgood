package failgood

interface ResourcesDSL {
    /**
     * asynchronously create a dependency. This is great for blocking dependencies, like a docker container.
     * The creator lambda runs on the IO dispatcher to make a cpu thread free for a test
     */
    @Deprecated("this does not feel so great. create an issue if you really like it.")
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
}

@FailGoodDSL
interface ContextDSL<GivenType> : ResourcesDSL {
    suspend fun <ContextDependency> context(
        contextName: String,
        tags: Set<String> = setOf(),
        given: (suspend () -> ContextDependency)?,
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    )
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
     * define a pending test.
     */
    suspend fun pending(behaviorDescription: String, function: TestLambda = {})

    /**
     * Register a callback to be called after all tests have completed
     */
    fun afterSuite(function: suspend () -> Unit)
}
