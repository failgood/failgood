package failfast.mock

import failfast.FailFastException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

/**
 * create a mock for class Mock
 *
 * all method calls will return null. To define other results use [verify]
 */
inline fun <reified Mock : Any> mock() = mock(Mock::class)

fun <Mock : Any> mock(kClass: KClass<Mock>): Mock {
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
        Thread.currentThread().contextClassLoader,
        arrayOf(kClass.java),
        MockHandler(kClass)
    ) as Mock
}

/**
 * Define what to return when a method is called
 *
 * `whenever(mock) { methodCall(ignoredParameter) }.thenReturn("blah")`
 *
 * parameters that you pass to method calls are ignored, any invocation of methodCall will return "blah"
 *
 */
suspend fun <Mock : Any, Result : Any> whenever(mock: Mock, lambda: suspend Mock.() -> Result):
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

class MockException constructor(msg: String) : AssertionError(msg)

interface MockReplyRecorder<Type> {
    fun thenReturn(parameter: Type)
}

private data class MethodCall(val method: Method, val arguments: List<Any>) {
    override fun toString(): String {
        return "${method.name}(" + arguments.joinToString() + ")"
    }
}


private fun getHandler(mock: Any): MockHandler {
    return Proxy.getInvocationHandler(mock) as? MockHandler
        ?: throw FailFastException("error finding invocation handler. is ${mock::class} really a mock?")
}

private class MockHandler(private val kClass: KClass<*>) : InvocationHandler {
    val results = mutableMapOf<Method, Any>()
    val calls = mutableListOf<MethodCall>()
    override fun invoke(proxy: Any?, method: Method, arguments: Array<out Any>?): Any? {
        val nonCoroutinesArgs = cleanArguments(arguments)
        calls.add(MethodCall(method, nonCoroutinesArgs))
        return results[method]
    }

    fun defineResult(method: Method, result: Any) {
        results[method] = result
    }

    suspend fun <Mock : Any, Reply : Any> whenever(
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
            val call = MethodCall(method, cleanArguments(args))
            if (!calls.contains(call))
                throw MockException("expected call $call never happened. calls: ${calls.joinToString()}")
            return null
        }

    }

    class MockReplyRecorderImpl<Type : Any>(val mockHandler: MockHandler, val recordingHandler: RecordingHandler) :
        MockReplyRecorder<Type> {
        override fun thenReturn(parameter: Type) {
            val call = recordingHandler.call!!
            mockHandler.defineResult(call.method, parameter)
        }
    }

    class RecordingHandler : InvocationHandler {
        var call: MethodCall? = null
        override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
            call = MethodCall(method, cleanArguments(args))
            return null
        }
    }
}

private fun cleanArguments(arguments: Array<out Any>?) =
    (arguments?.toList() ?: listOf()).dropLastWhile { it is Continuation<*> }
