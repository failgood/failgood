package nanotest

import strikt.api.expectThat
import strikt.assertions.isTrue
import java.lang.management.ManagementFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun main() {
    val testFinished = CompletableFuture<Unit>()
    val suite = Suite()
    suite.context("nanotest bootstrap context") {
        test("firstTest") {
            expectThat(true).isTrue()
            testFinished.complete(Unit)
        }
    }
    testFinished.get(1, TimeUnit.SECONDS)
    val uptime = ManagementFactory.getRuntimeMXBean().uptime;
    println("finished after: ${uptime}ms")
}

fun test(testName: String, function: () -> Unit) {
    function()
}

class Suite {
    fun context(contextName: String, function: () -> Unit) {
        function()
    }
}
