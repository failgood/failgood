package failgood.pitest

import failgood.ContextProvider
import failgood.Failed
import failgood.ObjectContextProvider
import failgood.Pending
import failgood.Success
import failgood.Suite
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.ContextInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.pitest.testapi.AbstractTestUnit
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnit
import org.pitest.testapi.TestUnitFinder

object FailGoodTestUnitFinder : TestUnitFinder {
    @OptIn(DelicateCoroutinesApi::class)
    override fun findTestUnits(clazz: Class<*>): List<TestUnit> {
        val contextProvider =
            try {
                ContextProvider { ObjectContextProvider(clazz).getContexts() }
            } catch (e: Exception) {
                return listOf()
            }
        val tests = runBlocking {
            Suite(listOf(contextProvider)).findTests(GlobalScope, false).map { it.result }.awaitAll()
        }.filterIsInstance<ContextInfo>()
        return tests.flatMap { it.tests.entries }.map { FailGoodTestUnit(it.key, it.value, clazz) }
    }

    class FailGoodTestUnit(
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
                    is Pending -> rc.notifySkipped(description)
                }
            }
        }
    }
}
