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
    }
    testFinished.get(1, TimeUnit.SECONDS)
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    println("finished after: ${uptime}ms")
}


class Suite {
    val contexts = mutableListOf<TestContext>()
    fun context(contextName: String, function: TestContext.() -> Unit) {
        val testContext = TestContext()
        contexts.add(testContext)
        testContext.function()
    }

    fun awaitExecution() = SuiteResult(false, contexts.flatMap(TestContext::testFailures))
}

class TestContext {
    val testFailures = mutableListOf<TestFailure>()

    fun test(testName: String, function: () -> Unit) {
        try {
            function()
        } catch (e: AssertionError) {
            testFailures.add(TestFailure(testName, e))
        }
    }

}

data class SuiteResult(val allOk: Boolean, val failedTests: Collection<TestFailure>)

data class TestFailure(val name: String, val throwable: Throwable)
