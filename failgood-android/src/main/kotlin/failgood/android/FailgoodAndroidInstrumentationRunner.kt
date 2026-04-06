package failgood.android

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle

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
private const val REPORT_VALUE_RESULT_IGNORED = -3

class FailgoodAndroidInstrumentationRunner : Instrumentation() {
    private lateinit var arguments: Bundle

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        this.arguments = arguments ?: Bundle()
        start()
    }

    override fun onStart() {
        waitForIdleSync()

        val classLoader = checkNotNull(javaClass.classLoader)
        val result =
            FailgoodAndroidBootstrap(ReflectiveAndroidClassRunner(classLoader)).run(
                arguments =
                    AndroidArguments(
                        classArgument = arguments.getString(CLASS_ARGUMENT),
                        failgoodClassArgument = arguments.getString(FAILGOOD_CLASS_ARGUMENT),
                    ),
                reporter = InstrumentationReporter(this),
            )

        finish(
            if (result.isSuccess) Activity.RESULT_OK else Activity.RESULT_CANCELED,
            if (result.isSuccess) successBundle(result) else failureBundle(result.failure!!),
        )
    }

    private fun successBundle(result: AndroidRunResult): Bundle =
        Bundle().apply {
            putString(Instrumentation.REPORT_KEY_STREAMRESULT, "$SUCCESS_MARKER\n")
            putString("failgoodSuites", result.totalSuites.toString())
            putString("failgoodTests", result.totalTests.toString())
        }

    private fun failureBundle(throwable: Throwable): Bundle =
        Bundle().apply {
            putString(
                Instrumentation.REPORT_KEY_STREAMRESULT,
                "FAILGOOD_ANDROID_FAILED\n${throwable.stackTraceToString()}",
            )
        }
}

private class InstrumentationReporter(private val instrumentation: Instrumentation) :
    AndroidRunReporter {
    private var lastClassName: String? = null

    override fun testStarted(test: AndroidReportedTest, currentIndex: Int, totalTests: Int) {
        instrumentation.sendStatus(
            REPORT_VALUE_RESULT_START,
            baseBundle(test, currentIndex, totalTests).apply {
                putString(
                    Instrumentation.REPORT_KEY_STREAMRESULT,
                    if (lastClassName != test.className) "\n${test.className}:" else "",
                )
                lastClassName = test.className
            },
        )
    }

    override fun testPassed(test: AndroidReportedTest, currentIndex: Int, totalTests: Int) {
        instrumentation.sendStatus(
            REPORT_VALUE_RESULT_OK,
            baseBundle(test, currentIndex, totalTests).apply {
                putString(Instrumentation.REPORT_KEY_STREAMRESULT, ".")
            },
        )
    }

    override fun testFailed(
        test: AndroidReportedTest,
        currentIndex: Int,
        totalTests: Int,
        throwable: Throwable,
    ) {
        instrumentation.sendStatus(
            REPORT_VALUE_RESULT_FAILURE,
            baseBundle(test, currentIndex, totalTests).apply {
                putString(REPORT_KEY_STACK, throwable.stackTraceToString())
                putString(
                    Instrumentation.REPORT_KEY_STREAMRESULT,
                    "\nError in ${test.className}#${test.testName}:\n${throwable.stackTraceToString()}",
                )
            },
        )
    }

    override fun testIgnored(test: AndroidReportedTest, currentIndex: Int, totalTests: Int) {
        instrumentation.sendStatus(
            REPORT_VALUE_RESULT_IGNORED,
            baseBundle(test, currentIndex, totalTests).apply {
                putString(Instrumentation.REPORT_KEY_STREAMRESULT, "")
            },
        )
    }

    private fun baseBundle(
        test: AndroidReportedTest,
        currentIndex: Int,
        totalTests: Int,
    ): Bundle =
        Bundle().apply {
            putString(Instrumentation.REPORT_KEY_IDENTIFIER, REPORT_VALUE_ID)
            putInt(REPORT_KEY_NUM_TOTAL, currentIndex)
            putInt(REPORT_KEY_NUM_CURRENT, currentIndex)
            putString(REPORT_KEY_NAME_CLASS, test.className)
            putString(REPORT_KEY_NAME_TEST, test.testName)
        }
}
