package failfast.pitest

import failfast.ObjectContextProvider
import failfast.RootContext
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

        override fun execute(rc: ResultCollector?) {
        }

    }

}

