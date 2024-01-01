package failgood.pitest

import failgood.ContextProvider
import failgood.Failure
import failgood.ObjectContextProvider
import failgood.Skipped
import failgood.Success
import failgood.Suite
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.TestResults
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.pitest.testapi.*

object FailGoodTestUnitFinder : TestUnitFinder {
    @OptIn(DelicateCoroutinesApi::class)
    override fun findTestUnits(
        clazz: Class<*>,
        listener: TestUnitExecutionListener?
    ): List<TestUnit> {
        val contexts =
            try {
                ObjectContextProvider(clazz).getContexts()
            } catch (e: ExceptionInInitializerError) {
                return listOf()
            } catch (e: Exception) {
                return listOf()
            }
        val tests =
            runBlocking {
                    Suite(listOf(ContextProvider { contexts }))
                        .findTests(GlobalScope, false)
                        .awaitAll()
                }
                .filterIsInstance<TestResults>()
        return tests.flatMap { it.tests.entries }.map { FailGoodTestUnit(it.key, it.value, clazz) }
    }

    class FailGoodTestUnit(
        test: TestDescription,
        private val deferredResult: Deferred<TestPlusResult>,
        clazz: Class<*>
    ) : AbstractTestUnit(Description(test.niceString(), clazz)) {
        override fun execute(rc: ResultCollector) {
            runBlocking {
                rc.notifyStart(description)
                when (val result = deferredResult.await().result) {
                    is Success -> rc.notifyEnd(description)
                    is Failure -> rc.notifyEnd(description, result.failure)
                    is Skipped -> rc.notifySkipped(description)
                }
            }
        }
    }
}
