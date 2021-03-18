package failfast.mock

import failfast.FailFastException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

inline fun <reified T : Any> mock() = mock(T::class)

fun <T : Any> mock(kClass: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
        Thread.currentThread().contextClassLoader,
        arrayOf(kClass.java),
        MockHandler(kClass)
    ) as T
}

fun getCalls(mock: Any): List<MethodCall> = getHandler(mock).calls

suspend fun <T : Any> verify(mock: T, lambda: suspend T.() -> Unit) = getHandler(mock).verify(lambda)
class MockException(msg: String) : AssertionError(msg)

suspend fun <Mock : Any, Result : Any> whenever(
    mock: Mock,
    lambda: suspend Mock.() -> Result
): MockReplyRecorder<Result> = getHandler(mock).whenever<Mock, Result>(lambda)

data class MethodCall(val method: Method, val arguments: List<Any>) {
    override fun toString(): String {
        return "${method.name}(" + arguments.joinToString() + ")"
    }
}

interface MockReplyRecorder<Type> {
    fun thenReturn(parameter: Type)
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
        return MockReplyRecorderImpl<Reply>(this, recordingHandler)
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
