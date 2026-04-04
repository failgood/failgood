package failgood.android

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import failgood.ExecutionListener
import failgood.Failure
import failgood.Suite
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.FailedTestCollectionExecution

const val SUCCESS_MARKER = "FAILGOOD_ANDROID_OK"

private const val REPORT_VALUE_ID = "AndroidJUnitRunner"
private const val REPORT_KEY_NUM_TOTAL = "numtests"
private const val REPORT_KEY_NUM_CURRENT = "current"
private const val REPORT_KEY_NAME_CLASS = "class"
private const val REPORT_KEY_NAME_TEST = "test"
private const val REPORT_KEY_STACK = "stack"
private const val REPORT_VALUE_RESULT_START = 1
private const val REPORT_VALUE_RESULT_OK = 0
private const val REPORT_VALUE_RESULT_FAILURE = -2
private const val SYNTHETIC_TEST_NAME = "failgood suite"
private const val CLASS_ARGUMENT = "class"
private const val FAILGOOD_CLASS_ARGUMENT = "failgood.class"

class FailgoodAndroidInstrumentationRunner : Instrumentation() {
    private lateinit var arguments: Bundle

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments ?: Bundle()
        start()
    }

    override fun onStart() {
        waitForIdleSync()

        val result =
            runCatching {
                val classNames = selectedClassNames(arguments)
                check(classNames.isNotEmpty()) {
                    "No failgood test classes configured. Set instrumentation argument '$CLASS_ARGUMENT'."
                }

                val template =
                    Bundle().apply {
                        putString(REPORT_KEY_IDENTIFIER, REPORT_VALUE_ID)
                        putInt(REPORT_KEY_NUM_TOTAL, classNames.size)
                    }

                classNames.forEachIndexed { index, className ->
                    runClass(template, className, index + 1)
                }

                successBundle(classNames.size)
            }

        finish(
            if (result.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            result.getOrElse(::failureBundle),
        )
    }

    private fun runClass(template: Bundle, className: String, currentIndex: Int) {
        val testResult =
            Bundle(template).apply {
                putString(REPORT_KEY_NAME_CLASS, className)
                putString(REPORT_KEY_NAME_TEST, SYNTHETIC_TEST_NAME)
                putInt(REPORT_KEY_NUM_CURRENT, currentIndex)
                putString(REPORT_KEY_STREAMRESULT, "\n$className:")
            }
        sendStatus(REPORT_VALUE_RESULT_START, testResult)

        val listener = CountingExecutionListener()
        val classLoader = checkNotNull(javaClass.classLoader) { "Instrumentation class loader missing." }
        val suiteResult =
            Suite(listOf(classLoader.loadClass(className).kotlin)).run(
                parallelism = 1,
                silent = true,
                listener = listener,
            )
        check(listener.discovered > 0 || listener.started > 0 || listener.finished > 0) {
            "Failgood runner did not discover or execute any tests in $className."
        }

        firstFailure(suiteResult.failedRootContexts, suiteResult.failedTests)?.let { throwable ->
            testResult.putString(REPORT_KEY_STACK, throwable.stackTraceToString())
            testResult.putString(
                REPORT_KEY_STREAMRESULT,
                "\nError in $className#$SYNTHETIC_TEST_NAME:\n${throwable.stackTraceToString()}",
            )
            sendStatus(REPORT_VALUE_RESULT_FAILURE, testResult)
            throw throwable
        }

        testResult.putString(REPORT_KEY_STREAMRESULT, ".")
        sendStatus(REPORT_VALUE_RESULT_OK, testResult)
    }

    private fun successBundle(totalSuites: Int): Bundle =
        Bundle().apply {
            putString(REPORT_KEY_STREAMRESULT, "$SUCCESS_MARKER\n")
            putString("failgoodSuites", totalSuites.toString())
        }

    private fun failureBundle(throwable: Throwable): Bundle =
        Bundle().apply {
            putString(
                REPORT_KEY_STREAMRESULT,
                "FAILGOOD_ANDROID_FAILED\n${throwable.stackTraceToString()}",
            )
        }

    private fun selectedClassNames(arguments: Bundle): List<String> =
        (arguments.getString(FAILGOOD_CLASS_ARGUMENT) ?: arguments.getString(CLASS_ARGUMENT))
            ?.split(',')
            ?.map { it.substringBefore('#').trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()

    private fun firstFailure(
        failedRootContexts: List<FailedTestCollectionExecution>,
        failedTests: List<TestPlusResult>,
    ): Throwable? {
        val rootFailure = failedRootContexts.firstOrNull()?.failure
        if (rootFailure != null) return rootFailure
        return (failedTests.firstOrNull()?.result as? Failure)?.failure
    }
}

private class CountingExecutionListener : ExecutionListener {
    var discovered = 0
    var started = 0
    var finished = 0

    override suspend fun testDiscovered(testDescription: TestDescription) {
        discovered += 1
    }

    override suspend fun testStarted(testDescription: TestDescription) {
        started += 1
    }

    override suspend fun testFinished(testPlusResult: TestPlusResult) {
        finished += 1
    }
}
