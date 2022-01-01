package failgood

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
                get { result }.isA<Failed>().get { failure }.isA<AssertionError>()
            }
            get(SuiteResult::allTests).hasSize(3)
        }
    }
    testFinished.await()
    println("bootstrapped after: ${uptime()}ms")

    FailGood.runAllTests(true)

    // let's see how far we can get with one second. on CI everything is much slower so we double the time there
    val limit: Long = if (System.getenv("CI") != null) 2000 else 1000
    expectThat(ManagementFactory.getRuntimeMXBean().uptime).isLessThan(limit)
}
