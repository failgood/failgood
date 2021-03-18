package failfast.mock

import failfast.FailFastException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

fun getCalls(mock: Any): List<MethodCall> {
    return getHandler(mock).calls

}

private fun getHandler(mock: Any): Handler {
    return Proxy.getInvocationHandler(mock) as? Handler
        ?: throw FailFastException("error finding invocation handler. is ${mock::class} really a mock?")
}


internal fun <T : Any> whenever(mock: T, lambda: T.() -> Unit): MockReplyRecorder {
    val handler = getHandler(mock)
    val recordingHandler = RecordingHandler()
    @Suppress("UNCHECKED_CAST") val receiver = Proxy.newProxyInstance(
        Thread.currentThread().contextClassLoader,
        arrayOf(handler.kClass.java),
        recordingHandler
    ) as T
    receiver.lambda()
    return MockReplyRecorder(handler, recordingHandler)
}

class RecordingHandler : InvocationHandler {
    var call: MethodCall? = null
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        call = MethodCall(method, cleanArguments(args))
        return null
    }

}


internal class MockReplyRecorder(val handler: Handler, val recordingHandler: RecordingHandler) {
    fun thenReturn(parameter: Any) {
        val call = recordingHandler.call!!
        handler.defineResult(call.method, parameter)
    }

}

data class MethodCall(val method: Method, val arguments: List<Any>) {
    override fun toString(): String {
        return "${method.name}(" + arguments.joinToString() + ")"
    }
}

inline fun <reified T : Any> mock() = mock(T::class)
fun <T : Any> mock(kClass: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
        Thread.currentThread().contextClassLoader,
        arrayOf(kClass.java),
        Handler(kClass)
    ) as T
}

internal class Handler(internal val kClass: KClass<*>) : InvocationHandler {
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
}

private fun cleanArguments(arguments: Array<out Any>?) =
    (arguments?.toList() ?: listOf()).dropLastWhile { it is Continuation<*> }
