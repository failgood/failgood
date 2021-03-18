package failfast.mock

import failfast.FailFastException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

fun getCalls(mock: Any): List<MethodCall> {
    val invocationHandler = Proxy.getInvocationHandler(mock) as? Handler
        ?: throw FailFastException("error finding invocation handler. is ${mock::class} really a mock?")
    return invocationHandler.calls

}

data class MethodCall(val kotlinMethod: KCallable<*>, val arguments: List<Any>) {
    override fun toString(): String {
        return "${kotlinMethod.name}(" + arguments.joinToString() + ")"
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

internal class Handler(private val kClass: KClass<*>) : InvocationHandler {
    val calls = mutableListOf<MethodCall>()
    override fun invoke(proxy: Any?, method: Method, arguments: Array<out Any>?): Any? {
        val kotlinMethod = kClass.members.single { it.name == method.name }
        val nonCoroutinesArgs = (arguments?.toList() ?: listOf()).dropLastWhile { it is Continuation<*> }
        calls.add(MethodCall(kotlinMethod, nonCoroutinesArgs))
        return null
    }
}
