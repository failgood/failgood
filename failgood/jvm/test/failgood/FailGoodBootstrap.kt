package failgood

import failgood.internal.SysInfo.uptime
import failgood.internal.util.getEnv
import kotlinx.coroutines.CompletableDeferred
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import java.lang.management.ManagementFactory
import java.util.concurrent.CompletableFuture

suspend fun main() {
    val testFinished = CompletableDeferred<Unit>()
    val failingTestFinished = CompletableFuture<Throwable>()
    val results =
        Suite {
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
            context("child context") {
                context("grandchild context") {
                    test("failing test") {
                        expectThat(true).isFalse()
                    }
                }
            }
        }.run(silent = true)
    expectThat(results) {
        get(SuiteResult::allOk).isFalse()
        get(SuiteResult::failedTests).and {
            hasSize(2)
            all {
                get(TestPlusResult::test).get(TestDescription::testName).isEqualTo("failing test")
                get { result }.isA<Failure>().get { failure }.isA<AssertionError>()
            }
            get(SuiteResult::allTests).hasSize(3)
        }
    }
    testFinished.await()
    println("bootstrapped after: ${uptime()}")

    FailGood.runAllTests(true)

    // let's see how far we can get with one second.
    // on CI everything is much slower, especially on windows
    // and we don't want to randomly fail ci so lets use 20000 for now
    val limit: Long = if (getEnv("SLOW_CI") != null) 20000 else 1000
    expectThat(ManagementFactory.getRuntimeMXBean().uptime).isLessThan(limit)
}
