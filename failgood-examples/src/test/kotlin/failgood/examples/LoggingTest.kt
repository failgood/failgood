package failgood.examples

import failgood.Test
import failgood.TestDSL
import failgood.describe
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.slf4j.Logger
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

@Test
class LoggingTest {
    val context = describe("Logging support") {
        it("injects a logger") {
            LoggingComponent(kLogger()).functionThatLogs()
        }
    }
}

fun TestDSL.kLogger() = KotlinLogging.logger(logger())
fun TestDSL.logger(): Logger {
    return Proxy.newProxyInstance(
        Thread.currentThread().contextClassLoader,
        arrayOf(KLogger::class.java),
        LoggingHandler(this)
    ) as KLogger
}

class LoggingHandler(private val resourcesDSL: TestDSL) : InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any {
        val methodName = method!!.name
        if (methodName.startsWith("is")) // isXXXEnabled()
            return true
        val message = buildString {
            args?.forEach {
                append(
                    when (it) {
                        is String -> it
                        else -> throw RuntimeException(
                            "$methodName parameters have unexpected type: " +
                                    "${it.javaClass} ${it.javaClass.superclass}"
                        )
                    }
                )
            }
        }
        runBlocking {
            resourcesDSL._test_event(methodName.uppercase(Locale.getDefault()), message)
        }
        return Unit
    }
}
