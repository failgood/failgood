package failfast.pitest

import failfast.Failed
import failfast.Ignored
import failfast.ObjectContextProvider
import failfast.RootContext
import failfast.Success
import failfast.Suite
import org.pitest.testapi.AbstractTestUnit
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnit
import org.pitest.testapi.TestUnitFinder

object FailFastTestUnitFinder : TestUnitFinder {
    override fun findTestUnits(clazz: Class<*>): List<TestUnit> {
        val description = Description("fail fast context")
        return listOf(FailFastTestUnit(ObjectContextProvider(clazz).getContext(), description))
    }

    class FailFastTestUnit(val context: RootContext, description: Description) : AbstractTestUnit(description) {

        override fun execute(rc: ResultCollector) {
            val result = Suite(context, 1).run()
            result.allTests.forEach {
                val description = Description(it.test.toString())
                rc.notifyStart(description)
                when (it) {
                    is Success -> rc.notifyEnd(description)
                    is Failed -> rc.notifyEnd(description, it.failure)
                    is Ignored -> rc.notifySkipped(description)
                }
            }
        }

    }

}

