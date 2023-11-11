package failgood.junit.exp.events

import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.it.JunitPlatformFunctionalTest.Results
import failgood.junit.it.JunitPlatformFunctionalTest.TEListener
import failgood.junit.it.fixtures.SimpleClassTestFixture
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Supplier
import kotlinx.coroutines.withTimeout
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.LoggingListener

suspend fun main() {
    execute(listOf(selectClass(SimpleClassTestFixture::class.java)))
}

suspend fun execute(selectors: List<DiscoverySelector>) {
    val listener = TEListener()
    LauncherFactory.create()
        .execute(
            LauncherDiscoveryRequestBuilder.request()
                .configurationParameters(
                    mapOf(FailGoodJunitTestEngineConstants.CONFIG_KEY_RUN_TEST_FIXTURES to "true")
                )
                .selectors(selectors)
                .build(),
            listener,
            printingListener()
        )
    // await with timeout to make sure the test does not hang
    val rootResult = withTimeout(5000) { listener.rootResult.await() }
    Results(rootResult, listener.results, listener.testEvents)
}

fun printingListener(): LoggingListener = LoggingListener.forBiConsumer(TestEventsPrinter())

class TestEventsPrinter : BiConsumer<Throwable?, Supplier<String>> {
    private val counter = AtomicInteger()

    override fun accept(t: Throwable?, u: Supplier<String>) {
        println("${counter.incrementAndGet()}-${u.get()}")
    }
}
