package failgood

interface ResourcesDSL {
    /**
     * asynchronously create a dependency. This is great for blocking dependencies, like a docker container.
     * The creator lambda runs on the IO dispatcher to make a cpu thread free for a test
     */
    suspend fun <T> dependency(creator: suspend () -> T, closer: suspend (T) -> Unit = {}): TestDependency<T>

    /**
     * create a test dependency that should be closed after the a test run.
     * use this instead of beforeEach/afterEach
     */
    fun <T : AutoCloseable> autoClose(wrapped: T): T

    /**
     * create a test dependency that should be closed after the a test run.
     * use this instead of beforeEach/afterEach
     */
    fun <T> autoClose(wrapped: T, closeFunction: suspend (T) -> Unit): T
}

@FailGoodDSL
interface ContextDSL : ResourcesDSL {
    /**
     * define a test context that describes a subject.
     */
    suspend fun describe(name: String, function: ContextLambda)

    /**
     * define a test that describes one aspect of a subject.
     */
    suspend fun it(behaviorDescription: String, function: TestLambda)

    /**
     * define a test context. if possible prefer [describe] with a description of behavior.
     */
    suspend fun context(name: String, function: ContextLambda)

    /**
     * define a test. [it] is probably better suited.
     */
    suspend fun test(name: String, function: TestLambda)

    /**
     * define a pending test.
     */
    suspend fun pending(behaviorDescription: String, function: TestLambda = {})

}
