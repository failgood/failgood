package failgood.mock

import failgood.FailGoodException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

/**
 * create a mock for class Mock
 *
 * per default all method calls will return null. To define other results use [whenever]
 */
inline fun <reified Mock : Any> mock() = mock(Mock::class)

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

/**
 * Define what to return when a method is called
 *
 * `whenever(mock) { methodCall(ignoredParameter) }.then {"resultString"}`
 * or
 * `whenever(mock) { methodCall(ignoredParameter) }.then { throw BlahException() }`
 *
 * parameters that you pass to method calls are ignored, any invocation of methodCall will return "blah"
 *
 */
suspend fun <Mock : Any, Result> whenever(mock: Mock, lambda: suspend Mock.() -> Result):
    MockReplyRecorder<Result> = getHandler(mock).whenever(lambda)

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

interface MockReplyRecorder<Type> {
    @Deprecated(message = "use then")
    fun thenReturn(parameter: Type)
    fun then(result: () -> Type)
}

private data class MethodWithArguments(val method: Method, val arguments: List<Any?>) {
    override fun toString(): String {
        return "${method.name}(" + arguments.joinToString() + ")"
    }
}

private fun getHandler(mock: Any): MockHandler {
    return Proxy.getInvocationHandler(mock) as? MockHandler
        ?: throw FailGoodException("error finding invocation handler. is ${mock::class} really a mock?")
}

private class MockHandler(private val kClass: KClass<*>) : InvocationHandler {
    val results = mutableMapOf<Method, () -> Any?>()
    val calls = CopyOnWriteArrayList<MethodWithArguments>()
    override fun invoke(proxy: Any, method: Method, arguments: Array<out Any>?): Any? {
        val nonCoroutinesArgs = cleanArguments(arguments)
        calls.add(MethodWithArguments(method, nonCoroutinesArgs))
        val result = results[method]
        if (result == null) {
            if (method.name == "equals")
                return proxy === arguments?.singleOrNull()
            else if (method.name == "toString" && nonCoroutinesArgs.isEmpty())
                return "mock<${kClass.simpleName}>"
        }
        return result?.invoke()
    }

    fun <T> defineResult(method: Method, result: () -> T) {
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
        val calls = mockHandler.calls
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
            val call = MethodWithArguments(method, cleanArguments(args))
            if (!calls.contains(call))
                throw MockException("expected call $call never happened. calls: ${calls.joinToString()}")
            return null
        }
    }

    class MockReplyRecorderImpl<Type>(val mockHandler: MockHandler, val recordingHandler: RecordingHandler) :
        MockReplyRecorder<Type> {
        @Suppress("OverridingDeprecatedMember")
        override fun thenReturn(parameter: Type) {
            val call = recordingHandler.call!!
            if (parameter != null)
                mockHandler.defineResult(call.method) { parameter }
        }

        override fun then(result: () -> Type) {
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
