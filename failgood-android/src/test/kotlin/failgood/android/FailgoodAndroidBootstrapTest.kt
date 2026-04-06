package failgood.android

import failgood.Context
import failgood.ExecutionListener
import failgood.Failure
import failgood.Ignored
import failgood.SourceInfo
import failgood.Success
import failgood.Test
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.TestFixture
import failgood.testCollection
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

@Test
class FailgoodAndroidBootstrapTest {
    val tests =
        testCollection("android bootstrap") {
            it("parses selected class names") {
                val arguments =
                    AndroidArguments(
                        classArgument = "other.Class",
                        failgoodClassArgument =
                            " some.Class , some.Class#test name , other.Class ",
                    )

                assert(arguments.classNames == listOf("some.Class", "other.Class"))
            }

            it("runs a passing failgood suite class") {
                val result =
                    ReflectiveAndroidClassRunner(checkNotNull(javaClass.classLoader))
                        .runClass(PassingSuite::class.java.name, NoopExecutionListener)

                assert(result.ranTests)
                assert(result.discoveredTests == 1)
                assert(result.failure == null)
            }

            it("returns the first failing test from a suite class") {
                val result =
                    ReflectiveAndroidClassRunner(checkNotNull(javaClass.classLoader))
                        .runClass(FailingSuite::class.java.name, NoopExecutionListener)

                assert(result.ranTests)
                assert(result.discoveredTests == 1)
                assert(result.failure is AssertionError)
            }

            it("fails when no classes are configured") {
                val result =
                    FailgoodAndroidBootstrap(FakeAndroidClassRunner(emptyMap()))
                        .run(AndroidArguments(), RecordingAndroidRunReporter())

                assert(
                    result.failure?.message ==
                        "No failgood test classes configured. Set instrumentation argument 'class'."
                )
            }

            it("reports each failgood test individually") {
                val first = testDescription("suite.One", "root", "test 1")
                val second = testDescription("suite.One", "root", "test 2")
                val reporter = RecordingAndroidRunReporter()
                val result =
                    FailgoodAndroidBootstrap(
                            FakeAndroidClassRunner(
                                mapOf(
                                    "suite.One" to
                                        FakeClassRun(
                                            discoveredTests = 2,
                                            execute = { listener ->
                                                listener.testDiscovered(first)
                                                listener.testStarted(first)
                                                listener.testFinished(
                                                    TestPlusResult(first, Success(1))
                                                )
                                                listener.testDiscovered(second)
                                                listener.testStarted(second)
                                                listener.testFinished(
                                                    TestPlusResult(second, Success(1))
                                                )
                                            },
                                        )
                                )
                            )
                        )
                        .run(AndroidArguments(classArgument = "suite.One"), reporter)

                assert(result == AndroidRunResult(totalSuites = 1, totalTests = 2))
                assert(
                    reporter.events ==
                        listOf(
                            "start:1/1:suite.One#root > test 1",
                            "pass:1/1:suite.One#root > test 1",
                            "start:2/2:suite.One#root > test 2",
                            "pass:2/2:suite.One#root > test 2",
                        )
                )
            }

            it("reports skipped tests as ignored") {
                val classRunner = ReflectiveAndroidClassRunner(checkNotNull(javaClass.classLoader))
                val reporter = RecordingAndroidRunReporter()
                val result =
                    FailgoodAndroidBootstrap(
                            FakeAndroidClassRunner(
                                mapOf(
                                    IgnoredSuite::class.java.name to
                                        FakeClassRun(
                                            discoveredTests = 1,
                                            execute = { listener ->
                                                val result =
                                                    classRunner.runClass(
                                                        IgnoredSuite::class.java.name,
                                                        listener,
                                                    )
                                                assert(result.ranTests)
                                            },
                                        )
                                )
                            )
                        )
                        .run(
                            AndroidArguments(classArgument = IgnoredSuite::class.java.name),
                            reporter,
                        )

                assert(result == AndroidRunResult(totalSuites = 1, totalTests = 1))
                assert(
                    reporter.events ==
                        listOf(
                            "start:1/1:${IgnoredSuite::class.java.name}#IgnoredSuite: ignored suite > is ignored",
                            "ignored:1/1:${IgnoredSuite::class.java.name}#IgnoredSuite: ignored suite > is ignored",
                        )
                )
            }

            it("continues reporting after a failing suite") {
                val first = testDescription("suite.One", "root", "test 1")
                val failing = testDescription("suite.Two", "root", "failing test")
                val last = testDescription("suite.Three", "root", "test 3")
                val failure = AssertionError("boom")
                val reporter = RecordingAndroidRunReporter()
                val result =
                    FailgoodAndroidBootstrap(
                            FakeAndroidClassRunner(
                                mapOf(
                                    "suite.One" to
                                        FakeClassRun(
                                            discoveredTests = 1,
                                            execute = { listener ->
                                                listener.testDiscovered(first)
                                                listener.testStarted(first)
                                                listener.testFinished(
                                                    TestPlusResult(first, Success(1))
                                                )
                                            },
                                        ),
                                    "suite.Two" to
                                        FakeClassRun(
                                            discoveredTests = 1,
                                            failure = failure,
                                            execute = { listener ->
                                                listener.testDiscovered(failing)
                                                listener.testStarted(failing)
                                                listener.testFinished(
                                                    TestPlusResult(failing, Failure(failure))
                                                )
                                            },
                                        ),
                                    "suite.Three" to
                                        FakeClassRun(
                                            discoveredTests = 1,
                                            execute = { listener ->
                                                listener.testDiscovered(last)
                                                listener.testStarted(last)
                                                listener.testFinished(
                                                    TestPlusResult(last, Success(1))
                                                )
                                            },
                                        ),
                                )
                            )
                        )
                        .run(
                            AndroidArguments(classArgument = "suite.One,suite.Two,suite.Three"),
                            reporter,
                        )

                assert(result.failure === failure)
                assert(result.totalTests == 3)
                assert(
                    reporter.events ==
                        listOf(
                            "start:1/1:suite.One#root > test 1",
                            "pass:1/1:suite.One#root > test 1",
                            "start:2/2:suite.Two#root > failing test",
                            "fail:2/2:suite.Two#root > failing test:boom",
                            "start:3/3:suite.Three#root > test 3",
                            "pass:3/3:suite.Three#root > test 3",
                        )
                )
            }
        }
}

private fun testDescription(className: String, contextName: String, testName: String) =
    TestDescription(Context(contextName), testName, SourceInfo(className, "$className.kt", 1))

private class RecordingAndroidRunReporter : AndroidRunReporter {
    val events = mutableListOf<String>()

    override fun testStarted(test: AndroidReportedTest, currentIndex: Int, totalTests: Int) {
        events += "start:$currentIndex/$totalTests:${test.className}#${test.testName}"
    }

    override fun testPassed(test: AndroidReportedTest, currentIndex: Int, totalTests: Int) {
        events += "pass:$currentIndex/$totalTests:${test.className}#${test.testName}"
    }

    override fun testFailed(
        test: AndroidReportedTest,
        currentIndex: Int,
        totalTests: Int,
        throwable: Throwable,
    ) {
        events += "fail:$currentIndex/$totalTests:${test.className}#${test.testName}:${throwable.message}"
    }

    override fun testIgnored(
        test: AndroidReportedTest,
        currentIndex: Int,
        totalTests: Int,
    ) {
        events += "ignored:$currentIndex/$totalTests:${test.className}#${test.testName}"
    }
}

private data class FakeClassRun(
    val discoveredTests: Int,
    val failure: Throwable? = null,
    val execute: suspend (ExecutionListener) -> Unit = {},
)

private class FakeAndroidClassRunner(private val results: Map<String, FakeClassRun>) :
    AndroidClassRunner {
    override fun runClass(className: String, listener: ExecutionListener): AndroidClassRunResult {
        val result = results.getValue(className)
        runSuspending { result.execute(listener) }
        return AndroidClassRunResult(className, result.discoveredTests, result.failure)
    }
}

private object NoopExecutionListener : ExecutionListener

private fun runSuspending(block: suspend () -> Unit) {
    block.startCoroutine(
        object : kotlin.coroutines.Continuation<Unit> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                result.getOrThrow()
            }
        }
    )
}

@TestFixture
object PassingSuite {
    val tests =
        testCollection("passing suite") {
            it("passes") { assert(true) }
        }
}

@TestFixture
object FailingSuite {
    val tests =
        testCollection("failing suite") {
            it("fails") { assert(false) }
        }
}

@TestFixture
object IgnoredSuite {
    val tests =
        testCollection("ignored suite") {
            it("is ignored", ignored = Ignored.Because("not today")) { assert(false) }
        }
}
