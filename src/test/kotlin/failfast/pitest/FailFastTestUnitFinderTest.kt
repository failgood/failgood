package failfast.pitest

import failfast.ObjectContextProvider
import failfast.RootContext
import failfast.describe
import org.pitest.testapi.AbstractTestUnit
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnit
import org.pitest.testapi.TestUnitFinder
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object FailFastTestUnitFinderTest {
    val context = describe(FailFastTestUnitFinder::class) {
        test("finds test contexts") {
            val finder: TestUnitFinder = FailFastTestUnitFinder()
            val testUnit: FailFastTestUnit =
                finder.findTestUnits(FailFastTestPluginFactoryTest::class.java).single() as FailFastTestUnit
            expectThat(testUnit.context).isEqualTo(FailFastTestPluginFactoryTest.context)
        }
    }
}

class FailFastTestUnitFinder : TestUnitFinder {
    override fun findTestUnits(clazz: Class<*>): List<TestUnit> {
        val description = Description("fail fast context")
        return listOf(FailFastTestUnit(ObjectContextProvider(clazz).getContext(), description))
    }

}

class FailFastTestUnit(val context: RootContext, description: Description) : AbstractTestUnit(description) {

    override fun execute(rc: ResultCollector?) {
    }

}
