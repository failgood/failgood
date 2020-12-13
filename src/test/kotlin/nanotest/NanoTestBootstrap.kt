package nanotest

import strikt.api.expectThat
import strikt.assertions.isFalse
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
        test("failing test") {
            expectThat(true).isFalse()
        }
    }
    val results = suite.awaitExecution()
    expectThat(results.allOk).isFalse()
    testFinished.get(1, TimeUnit.SECONDS)
    val uptime = ManagementFactory.getRuntimeMXBean().uptime;
    println("finished after: ${uptime}ms")
}

fun test(testName: String, function: () -> Unit) {
    try {
        function()
    } catch (e: AssertionError) {
    }
}

class Suite {
    fun context(contextName: String, function: () -> Unit) {
        function()
    }

    fun awaitExecution() = SuiteResult(false)
}

data class SuiteResult(val allOk: Boolean)
