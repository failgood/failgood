package failgood.dsl

import failgood.Ignored

/**
 * This is used to define test contexts and tests. It is recommended to use [describe]/[it]-syntax,
 * but if you really have to you can also use [context] to define a context and [test] to define a
 * test
 */
@FailGoodDSL
interface ContextDSL<GivenType> : ResourcesDSL {
    /**
     * Define a context that describes a subject with a given block. set [isolation] false to turn
     * off test isolation for this context. the [given] block will be called for every test and
     * passed as argument, even if the context has isolation off the whole context can be set to be
     * ignored by setting [ignored] to `Because(<Reason for ignoring>)`, see [Ignored]
     */
    suspend fun <ContextDependency> describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        ignored: Ignored? = null,
        given: (suspend () -> ContextDependency),
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    )

    /**
     * Define a test context that describes a subject. this is a helper function for contexts
     * without a given block
     */
    suspend fun describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        ignored: Ignored? = null,
        function: ContextLambda
    ) = describe(name, tags, isolation, ignored, {}, function)

    /**
     * Define a test that describes one aspect of a subject.
     *
     * examples:
     * ```
     * // just a normal test
     * it("can fly") {...}
     * // A test that is disabled for now.
     * it("can fly", disabled=Because("this test only shows how the API should look like but nothing is implemented yet")) {...}
     *
     * ```
     */
    suspend fun it(
        name: String,
        tags: Set<String> = setOf(),
        ignored: Ignored? = null,
        function: TestLambda<GivenType>
    )

    /** Register a callback to be called after all tests have completed */
    fun afterSuite(function: suspend () -> Unit)

    /**
     * Define a context with a given block. The given block will be called for every test and passed
     * as argument, even if isolation is turned off.
     */
    suspend fun <ContextDependency> context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        given: (suspend () -> ContextDependency),
        ignored: Ignored? = null,
        contextLambda: suspend ContextDSL<ContextDependency>.() -> Unit
    ) = describe(name, tags, isolation, ignored, given, contextLambda)

    /** Define a test context. Prefer [describe] with a description of behavior. */
    suspend fun context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        ignored: Ignored? = null,
        function: ContextLambda
    ) = describe(name, tags, isolation, ignored, function)

    /** Define a test. Prefer [it] */
    suspend fun test(
        name: String,
        tags: Set<String> = setOf(),
        ignored: Ignored? = null,
        function: TestLambda<GivenType>
    ) = it(name, tags, ignored, function)
}

internal typealias ContextLambda = suspend ContextDSL<Unit>.() -> Unit

internal typealias TestLambda<GivenType> = suspend TestDSL.(GivenType) -> Unit