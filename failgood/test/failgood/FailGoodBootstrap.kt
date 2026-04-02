package failgood

import failgood.internal.sysinfo.uptime
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CompletableDeferred

suspend fun main() {
    val testFinished = CompletableDeferred<Unit>()
    val failingTestFinished = CompletableFuture<Throwable>()
    val results =
        Suite {
                test("firstTest") {
                    assert(true)
                    testFinished.complete(Unit)
                }
                test("failing test") {
                    try {
                        assert(!true)
                    } catch (e: AssertionError) {
                        failingTestFinished.complete(e)
                        throw e
                    }
                }
                context("child context") {
                    context("grandchild context") { test("failing test") { assert(!true) } }
                }
            }
            .run(silent = true)
    assert(!results.allOk)
    assert(results.failedTests.size == 2)
    results.failedTests.forEach { failedTest ->
        assert(failedTest.test.testName == "failing test")
        assert(failedTest.result is Failure)
        assert((failedTest.result as Failure).failure is AssertionError)
    }
    assert(results.allTests.size == 3)
    testFinished.await()
    println("bootstrapped after: ${uptime()}")

    FailGood.runAllTests(false, silent = true)

    // let's see how far we can get with one second.
    // on CI everything is much slower, especially on windows,
    // and we don't want to randomly fail ci so lets use 20000 for now
    /* disabling for now because gradle runs this in parallel to other tasks,and then it fails.
    also it will probably fail on slow computers
     val limit: Long = if (getenv("SLOW_CI") != null) 20000 else 1000
     expectThat(ManagementFactory.getRuntimeMXBean().uptime).isLessThan(limit)*/
}
