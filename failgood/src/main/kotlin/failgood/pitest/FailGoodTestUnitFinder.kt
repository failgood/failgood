package failgood.pitest

import failgood.*
import kotlinx.coroutines.*
import org.pitest.testapi.*

object FailGoodTestUnitFinder : TestUnitFinder {
    @OptIn(DelicateCoroutinesApi::class)
    override fun findTestUnits(clazz: Class<*>): List<TestUnit> {
        val contextProvider: ObjectContextProvider =
            try {
                ObjectContextProvider(clazz).apply { getContexts() }
            } catch (e: Exception) {
                return listOf()
            }
        val tests = runBlocking { Suite(listOf(contextProvider)).findTests(GlobalScope, false).awaitAll() }
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
