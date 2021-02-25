package failfast.pitest

import failfast.Failed
import failfast.Ignored
import failfast.ObjectContextProvider
import failfast.Success
import failfast.Suite
import failfast.TestDescription
import failfast.TestPlusResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
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
        val tests = runBlocking { Suite(rootContext).findTests(GlobalScope, false).awaitAll() }
        return tests.flatMap { it.tests.entries }.map { FailFastTestUnit(it.key, it.value, clazz) }
    }

    class FailFastTestUnit(
        test: TestDescription,
        private val deferredResult: Deferred<TestPlusResult>,
        clazz: Class<*>
    ) : AbstractTestUnit(Description(test.toString(), clazz)) {
        override fun execute(rc: ResultCollector) {
            runBlocking {
                rc.notifyStart(description)
                when (val result = deferredResult.await().result) {
                    is Success -> rc.notifyEnd(description)
                    is Failed -> rc.notifyEnd(description, result.failure)
                    is Ignored -> rc.notifySkipped(description)
                }
            }
        }
    }
}
