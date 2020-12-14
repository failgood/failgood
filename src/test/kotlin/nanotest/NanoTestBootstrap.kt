package nanotest

import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isLessThan
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import strikt.assertions.single
import java.lang.management.ManagementFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun main() {
    val testFinished = CompletableFuture<Unit>()
    val failingTestFinished = CompletableFuture<Throwable>()
    val results = Suite() {
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
    }.run()
    expectThrows<RuntimeException> { results.check() }
    expectThat(results) {
        get(SuiteResult::allOk).isFalse()
        get(SuiteResult::failedTests).hasSize(1).single().and {
            get(TestFailure::name).isEqualTo("failing test")
            get(TestFailure::throwable).isSameInstanceAs(failingTestFinished.get())
        }
        get(SuiteResult::contexts).hasSize(1).single().and {
            get(TestContext::name).isEqualTo("root")
        }
    }
    testFinished.get(1, TimeUnit.SECONDS)

    Suite(listOf(ContextLifecycleTest.context, SuiteTest.context)).run().check()
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    println("finished after: ${uptime}ms")
    expectThat(uptime).isLessThan(1000) // lets see how far we can get with one second
}



