package failgood.android

import failgood.ExecutionListener
import failgood.Failure
import failgood.Suite
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.FailedTestCollectionExecution

internal const val CLASS_ARGUMENT = "class"
internal const val FAILGOOD_CLASS_ARGUMENT = "failgood.class"

internal data class AndroidArguments(
    val classArgument: String? = null,
    val failgoodClassArgument: String? = null
) {
    val classNames: List<String> =
        (failgoodClassArgument ?: classArgument)
            ?.split(',')
            ?.map { it.substringBefore('#').trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()
}

internal data class AndroidReportedTest(val className: String, val testName: String)

internal data class AndroidClassRunResult(
    val suiteClassName: String,
    val discoveredTests: Int,
    val failure: Throwable? = null
) {
    val ranTests: Boolean = discoveredTests > 0
}

internal data class AndroidRunResult(
    val totalSuites: Int = 0,
    val totalTests: Int = 0,
    val failure: Throwable? = null
) {
    val isSuccess: Boolean = failure == null
}

internal interface AndroidRunReporter {
    fun testStarted(test: AndroidReportedTest, currentIndex: Int, totalTests: Int)

    fun testPassed(test: AndroidReportedTest, currentIndex: Int, totalTests: Int)

    fun testFailed(
        test: AndroidReportedTest,
        currentIndex: Int,
        totalTests: Int,
        throwable: Throwable,
    )

    fun testIgnored(test: AndroidReportedTest, currentIndex: Int, totalTests: Int)
}

internal interface AndroidClassRunner {
    fun runClass(className: String, listener: ExecutionListener): AndroidClassRunResult
}

internal class FailgoodAndroidBootstrap(
    private val classFinder: AndroidTestClassFinder,
    private val classRunner: AndroidClassRunner,
) {
    fun run(arguments: AndroidArguments, reporter: AndroidRunReporter): AndroidRunResult {
        val classNames = arguments.classNames.ifEmpty(classFinder::findTestClasses)
        if (classNames.isEmpty()) {
            return AndroidRunResult(
                failure = IllegalStateException("No failgood test classes configured or discovered.")
            )
        }
        val reportingState = ReportingState()
        val results =
            classNames.map { className ->
                classRunner.runClass(
                    className,
                    StreamingExecutionListener(className, reportingState, reporter),
                )
            }
        val totalTests = results.sumOf { it.discoveredTests }
        val firstFailure =
            results.firstNotNullOfOrNull(::firstFailure)
        return AndroidRunResult(
            totalSuites = results.size,
            totalTests = totalTests,
            failure = firstFailure,
        )
    }

    private fun firstFailure(classResult: AndroidClassRunResult): Throwable? {
        return when {
            !classResult.ranTests ->
                IllegalStateException(
                    "Failgood runner did not discover or execute any tests in ${classResult.suiteClassName}."
                )

            else -> classResult.failure
        }
    }
}

internal class ReflectiveAndroidClassRunner(private val classLoader: ClassLoader) : AndroidClassRunner {
    override fun runClass(className: String, listener: ExecutionListener): AndroidClassRunResult {
        return runCatching {
                val suiteResult =
                    Suite(listOf(classLoader.loadClass(className).kotlin)).run(
                        parallelism = 1,
                        silent = true,
                        listener = listener,
                    )
                AndroidClassRunResult(
                    suiteClassName = className,
                    discoveredTests = suiteResult.allTests.size,
                    failure = firstFailure(suiteResult.failedRootContexts, suiteResult.failedTests),
                )
            }
            .getOrElse {
                AndroidClassRunResult(
                    suiteClassName = className,
                    discoveredTests = 0,
                    failure = it,
                )
            }
    }

    private fun firstFailure(
        failedRootContexts: List<FailedTestCollectionExecution>,
        failedTests: List<TestPlusResult>,
    ): Throwable? {
        val rootFailure = failedRootContexts.firstOrNull()?.failure
        if (rootFailure != null) return rootFailure
        return (failedTests.firstOrNull()?.result as? Failure)?.failure
    }
}

private data class ReportingState(
    var discoveredTests: Int = 0,
    val testIndices: MutableMap<TestDescription, Int> = mutableMapOf(),
    val startedTests: MutableSet<TestDescription> = mutableSetOf(),
)

private class StreamingExecutionListener(
    private val suiteClassName: String,
    private val reportingState: ReportingState,
    private val reporter: AndroidRunReporter,
) : ExecutionListener {
    override suspend fun testDiscovered(testDescription: TestDescription) {
        reportingState.discoveredTests += 1
        reportingState.testIndices[testDescription] = reportingState.discoveredTests
    }

    override suspend fun testStarted(testDescription: TestDescription) {
        val reportedTest = testDescription.toReportedTest(suiteClassName)
        val currentIndex = reportingState.testIndices.getValue(testDescription)
        reporter.testStarted(reportedTest, currentIndex, reportingState.discoveredTests)
        reportingState.startedTests += testDescription
    }

    override suspend fun testFinished(testPlusResult: TestPlusResult) {
        val testDescription = testPlusResult.test
        val reportedTest = testDescription.toReportedTest(suiteClassName)
        val currentIndex = reportingState.testIndices.getValue(testDescription)
        if (reportingState.startedTests.add(testDescription)) {
            reporter.testStarted(reportedTest, currentIndex, reportingState.discoveredTests)
        }
        when (val result = testPlusResult.result) {
            is Failure -> reporter.testFailed(reportedTest, currentIndex, reportingState.discoveredTests, result.failure)

            else ->
                if (testPlusResult.isSkipped) {
                    reporter.testIgnored(reportedTest, currentIndex, reportingState.discoveredTests)
                } else {
                    reporter.testPassed(reportedTest, currentIndex, reportingState.discoveredTests)
                }
        }
    }
}

private fun TestDescription.toReportedTest(suiteClassName: String) =
    AndroidReportedTest(suiteClassName, niceString())
