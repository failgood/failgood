package failgood.mock

import failgood.FailGoodException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

/**
 * create a mock
 *
 * per default all method calls will return null. To define other results use [the]
 */
inline fun <reified Mock : Any> mock() = mock(Mock::class)

/**
 * create a mock for class Mock and define its behavior
 * ```
 *                 val mock = mock<UserManager> {
 *                     method { stringReturningFunction() }.returns("resultString")
 *                     method { functionThatReturnsNullableString() }.will { "otherResultString" }
 *                 }```
 *
 */
suspend inline fun <reified Mock : Any> mock(noinline lambda: suspend MockConfigureDSL<Mock>.() -> Unit): Mock {
    return the(mock(Mock::class), lambda)
}

/**
 * define results for method invocations on a mock
 * ```
 *             val userManager: UserManager = mock()
 *             the(userManager) {
 *                 method { stringReturningFunction() }.returns("resultString")
 *             }```
 */
suspend fun <Mock : Any> the(
    mock: Mock,
    lambda: suspend MockConfigureDSL<Mock>.() -> Unit
): Mock {
    val dsl = MockConfigureDSLImpl(mock)
    dsl.lambda()
    return mock
}

/**
 * Verify mock invocations
 * ```
 * interface ManagerManager {
 *  fun manage(manager:String)
 * }
 * val mock = mock<ManagerManager>()
 * mock.manage("jakob")
 * verify(mock) { mock.manage("jakob") } // works
 * verify(mock) { mock.manage("jack") } // throws
 * ```
 *
 */
suspend fun <Mock : Any> verify(mock: Mock, lambda: suspend Mock.() -> Unit) {
    getHandler(mock).verify(lambda)
}

/**
 * Return calls to a mock to check with your favorite assertion lib (or the assertion lib you use) in combination with
 * the [call] helper.
 * This is an alternative to [verify]
 *
 * `expectThat(getCalls(mock)).single() .isEqualTo(call(Class::method, parameter1, parameter2, ...))`
 *
 */
fun getCalls(mock: Any) = getHandler(mock).calls.map { FunctionCall(it.method.name, it.arguments) }

data class FunctionCall(val function: String, val arguments: List<Any?>)

class MockException internal constructor(msg: String) : AssertionError(msg)
interface MockConfigureDSL<Mock> {
    suspend fun <Result> method(lambda: suspend Mock.() -> Result): MockReplyRecorder<Result>

    // these are just placeholders for mock setup.
    // currently you could use any string instead of anyString, because parameter values are not checked.
    // but use these to indicate to the reader that the values are not checked.
    @Suppress("UNCHECKED_CAST")
    fun <T> any(): T = null as T
    fun anyString(): String = ""
    fun anyByte(): Byte = 0
    fun anyShort(): Short = 0
    fun anyInt(): Int = 42
    fun anyLong(): Long = 0
    fun anyFloat(): Float = 0f
    fun anyDouble(): Double = 0.0
    fun anyBoolean(): Boolean = false
    fun anyChar(): Char = 'A'
}

private class MockConfigureDSLImpl<Mock : Any>(val mock: Mock) : MockConfigureDSL<Mock> {
    init {
        getHandler(mock)
    } // fail fast if the mock is not a mock

    override suspend fun <Result> method(lambda: suspend Mock.() -> Result):
        MockReplyRecorder<Result> = getHandler(mock).whenever(lambda)
}

fun <Mock : Any> mock(kClass: KClass<Mock>): Mock {
    @Suppress("UNCHECKED_CAST")
    return try {
        Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(kClass.java),
            MockHandler(kClass)
        )
    } catch (e: IllegalArgumentException) {
        throw FailGoodException(
            "error creating mock for ${kClass.qualifiedName}." +
                " This simple mocking lib can only mock interfaces."
        )
    } as Mock
}

interface MockReplyRecorder<Type> {
    fun returns(parameter: Type)
    fun will(result: (MethodWithArguments) -> Type)
}

data class MethodWithArguments(val method: Method, val arguments: List<Any?>) {
    override fun toString(): String {
        return "${method.name}(" + arguments.joinToString() + ")"
    }
}

internal fun getHandler(mock: Any): MockHandler {
    return try {
        Proxy.getInvocationHandler(mock)
    } catch (e: Exception) {
        null
    } as? MockHandler
        ?: throw MockException("error finding invocation handler. is ${mock::class} really a mock?")
}

internal class MockHandler(private val kClass: KClass<*>) : InvocationHandler {
    val results = mutableMapOf<Method, (MethodWithArguments) -> Any?>()
    internal val calls = CopyOnWriteArrayList<MethodWithArguments>()
    override fun invoke(proxy: Any, method: Method, arguments: Array<out Any>?): Any? {
        val nonCoroutinesArgs = cleanArguments(arguments)
        val methodWithArguments = MethodWithArguments(method, nonCoroutinesArgs)
        calls.add(methodWithArguments)
        val result = results[method]
        if (result == null) {
            if (method.name == "equals")
                return proxy === arguments?.singleOrNull()
            else if (method.name == "toString" && nonCoroutinesArgs.isEmpty())
                return "mock<${kClass.simpleName}>"
        }
        // if there is a result lambda defined, call that, otherwise return null
        return result?.invoke(methodWithArguments)
    }

    fun <T> defineResult(method: Method, result: (MethodWithArguments) -> T) {
        results[method] = result
    }

    suspend fun <Mock : Any, Reply> whenever(
        lambda: suspend Mock.() -> Reply
    ): MockReplyRecorder<Reply> {
        val recordingHandler = RecordingHandler()
        makeProxy<Mock>(recordingHandler).lambda()
        return MockReplyRecorderImpl(this, recordingHandler)
    }

    suspend fun <T : Any> verify(lambda: suspend T.() -> Unit) = makeProxy<T>(VerifyingHandler(this)).lambda()

    private fun <T : Any> makeProxy(handler: InvocationHandler): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(Thread.currentThread().contextClassLoader, arrayOf(kClass.java), handler) as T
    }

    class VerifyingHandler(mockHandler: MockHandler) : InvocationHandler {
        private val calls = mockHandler.calls
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
            val call = MethodWithArguments(method, cleanArguments(args))
            if (!calls.contains(call))
                throw MockException("expected call $call never happened. calls: ${calls.joinToString()}")
            return null
        }
    }

    class MockReplyRecorderImpl<Type>(
        private val mockHandler: MockHandler,
        private val recordingHandler: RecordingHandler
    ) :
        MockReplyRecorder<Type> {
            override fun returns(parameter: Type) {
                val call = recordingHandler.call!!
                if (parameter != null)
                    mockHandler.defineResult(call.method) { parameter }
            }

            override fun will(result: (MethodWithArguments) -> Type) {
                val call = recordingHandler.call!!
                mockHandler.defineResult(call.method, result)
            }
        }

    class RecordingHandler : InvocationHandler {
        var call: MethodWithArguments? = null
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
            call = MethodWithArguments(method, cleanArguments(args))
            return null
        }
    }
}

private fun cleanArguments(arguments: Array<out Any>?) =
    (arguments?.toList() ?: listOf()).dropLastWhile { it is Continuation<*> }
