package failgood.junit.jupiter

import failgood.describe
import failgood.junit.it.JunitPlatformFunctionalTest
import failgood.junit.jupiter.fixtures.JunitTest
import kotlinx.coroutines.withTimeout
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.LoggingListener
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import java.util.function.Supplier

// @Test // this test is currently for running manually, more like a main method. (maybe it should be a main method?)
object JupiterTest {
    val tests = describe("junit jupiter") {
        it("sends events") {
            execute(listOf(DiscoverySelectors.selectClass(JunitTest::class.java)))
        }
    }

    private suspend fun execute(discoverySelectors: List<DiscoverySelector>): JunitPlatformFunctionalTest.Results {
        val listener = JunitPlatformFunctionalTest.TEListener()
        LauncherFactory.create().execute(
            LauncherDiscoveryRequestBuilder.request()
                .selectors(discoverySelectors)
                .build(),
            listener,
            printingListener()

        )
        // await with timeout to make sure the test does not hang
        val rootResult = withTimeout(5000) { listener.rootResult.await() }
        return JunitPlatformFunctionalTest.Results(rootResult, listener.results)
    }

    fun printingListener(): LoggingListener? =
        LoggingListener.forBiConsumer(Printer())

    class Printer : BiConsumer<Throwable?, Supplier<String>> {
        val counter = AtomicInteger()
        override fun accept(t: Throwable?, u: Supplier<String>) {
            println("${counter.incrementAndGet()}-${u.get()}")
        }
    }
}
