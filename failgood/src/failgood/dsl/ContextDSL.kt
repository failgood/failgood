package failgood.dsl

import failgood.Ignored

/**
 * This is used to define test contexts and tests. It is recommended to use [describe]/[it]-syntax,
 * but you can also use [context] to define a context and [test] to define a test.
 */
@FailGoodDSL
interface ContextDSL<GivenType> : ResourcesDSL, ContextOnlyResourceDSL {
    /**
     * Define a context that describes a subject with a given block. Set [isolation] false to turn
     * off test isolation for this context.
     *
     * The [given] block will be called for every test and passed as argument, even if the context
     * has isolation off.
     *
     * The whole context can be set to be ignored by setting [ignored] to `Because(<Reason for
     * ignoring>)`, see [Ignored].
     */
    suspend fun <ContextGiven> describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        ignored: Ignored? = null,
        given: GivenFunction<GivenType, ContextGiven>,
        contextFunction: suspend ContextDSL<ContextGiven>.() -> Unit
    )

    /**
     * Define a test context that describes a subject. This is a helper function for contexts
     * without a given block.
     */
    suspend fun describe(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        ignored: Ignored? = null,
        contextFunction: ContextFunction
    ): Unit = describe(name, tags, isolation, ignored, {}, contextFunction)

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
        function: TestFunction<GivenType>
    )

    /** Register a callback to be called after all tests have completed */
    fun afterSuite(function: suspend () -> Unit)

    /** Define a context with a given block. See [describe] */
    suspend fun <ContextGiven> context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        given: GivenFunction<GivenType, ContextGiven>,
        ignored: Ignored? = null,
        contextFunction: suspend ContextDSL<ContextGiven>.() -> Unit
    ): Unit = describe(name, tags, isolation, ignored, given, contextFunction)

    /** Define a test context. see [describe] */
    suspend fun context(
        name: String,
        tags: Set<String> = setOf(),
        isolation: Boolean? = null,
        ignored: Ignored? = null,
        contextFunction: ContextFunction
    ): Unit = describe(name, tags, isolation, ignored, contextFunction)

    /** Define a test. see [it] */
    suspend fun test(
        name: String,
        tags: Set<String> = setOf(),
        ignored: Ignored? = null,
        function: TestFunction<GivenType>
    ): Unit = it(name, tags, ignored, function)
}

internal typealias ContextFunction = suspend ContextDSL<Unit>.() -> Unit

internal typealias ContextFunctionWithGiven<Given> = suspend ContextDSL<Given>.() -> Unit

internal typealias TestFunction<GivenType> = suspend TestDSLWithGiven<GivenType>.() -> Unit

internal typealias GivenFunction<ParentGivenType, GivenType> =
    suspend GivenDSL<ParentGivenType>.() -> GivenType

fun interface GivenDSL<ParentGivenType> {
    suspend fun given(): ParentGivenType
}
