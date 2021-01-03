package failfast.pitest

import failfast.Failed
import failfast.Ignored
import failfast.ObjectContextProvider
import failfast.Success
import failfast.Suite
import failfast.TestDescriptor
import failfast.TestResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.pitest.testapi.AbstractTestUnit
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnit
import org.pitest.testapi.TestUnitFinder

object FailFastTestUnitFinder : TestUnitFinder {
    override fun findTestUnits(clazz: Class<*>): List<TestUnit> {
        val rootContext =
            try {
                ObjectContextProvider(clazz).getContext()
            } catch (e: Exception) {
                return listOf()
            }
        val tests = runBlocking { Suite(rootContext).findTests(GlobalScope, false) }
        return tests.flatMap { it.tests.entries }.map { FailFastTestUnit(it.key, it.value, clazz) }
    }

    class FailFastTestUnit(
        test: TestDescriptor,
        private val deferredResult: Deferred<TestResult>,
        clazz: Class<*>
    ) : AbstractTestUnit(Description(test.toString(), clazz)) {
        override fun execute(rc: ResultCollector) {
            runBlocking {
                rc.notifyStart(description)
                when (val result = deferredResult.await()) {
                    is Success -> rc.notifyEnd(description)
                    is Failed -> rc.notifyEnd(description, result.failure)
                    is Ignored -> rc.notifySkipped(description)
                }
            }
        }
    }
}
