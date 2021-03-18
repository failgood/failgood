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
        Handler(kClass)
    ) as T
}

fun getCalls(mock: Any): List<MethodCall> = getHandler(mock).calls

fun <T : Any> verify(mock: T, lambda: T.() -> Unit) = getHandler(mock).verify(lambda)

fun <T : Any> whenever(mock: T, lambda: T.() -> Unit): MockReplyRecorder = getHandler(mock).whenever(lambda)

data class MethodCall(val method: Method, val arguments: List<Any>) {
    override fun toString(): String {
        return "${method.name}(" + arguments.joinToString() + ")"
    }
}

interface MockReplyRecorder {
    fun thenReturn(parameter: Any)
}

private fun getHandler(mock: Any): Handler {
    return Proxy.getInvocationHandler(mock) as? Handler
        ?: throw FailFastException("error finding invocation handler. is ${mock::class} really a mock?")
}

private class Handler(private val kClass: KClass<*>) : InvocationHandler {
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

    fun <T : Any> whenever(
        lambda: T.() -> Unit
    ): MockReplyRecorder {
        val recordingHandler = RecordingHandler()
        makeProxy<T>(recordingHandler).lambda()
        return MockReplyRecorderImpl(this, recordingHandler)
    }

    private fun <T : Any> makeProxy(recordingHandler: InvocationHandler): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(kClass.java),
            recordingHandler
        ) as T
    }

    fun <T : Any> verify(lambda: T.() -> Unit) = makeProxy<T>(VerifyingHandler()).lambda()

    class VerifyingHandler : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?) = null

    }

    class MockReplyRecorderImpl(val handler: Handler, val recordingHandler: RecordingHandler) : MockReplyRecorder {
        override fun thenReturn(parameter: Any) {
            val call = recordingHandler.call!!
            handler.defineResult(call.method, parameter)
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
