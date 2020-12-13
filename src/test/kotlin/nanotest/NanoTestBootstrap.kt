package nanotest

import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import strikt.assertions.single
import java.lang.management.ManagementFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun main() {
    val suite = Suite()
    val testFinished = CompletableFuture<Unit>()
    val failingTestFinished = CompletableFuture<Throwable>()
    suite.context("nanotest bootstrap context") {
        test("firstTest") {
            expectThat(true).isTrue()
            testFinished.complete(Unit)
        }
        test("failing test") {
            try {
                expectThat(true).isFalse()
            } catch (e: AssertionError) {
                failingTestFinished.complete(e)
                throw e
            }
        }
    }
    val results = suite.awaitExecution()
    expectThat(results) {
        get(SuiteResult::allOk).isFalse()
        get(SuiteResult::failedTests).hasSize(1).single().and {
            get(TestFailure::name).isEqualTo("failing test")
            get(TestFailure::throwable).isSameInstanceAs(failingTestFinished.get())
        }
        get(SuiteResult::contexts).hasSize(1).single().and {
            get(TestContext::name).isEqualTo("nanotest bootstrap context")
        }
    }
    testFinished.get(1, TimeUnit.SECONDS)
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    println("finished after: ${uptime}ms")
}


