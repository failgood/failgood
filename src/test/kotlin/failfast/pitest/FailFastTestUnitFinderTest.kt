package failfast.pitest

import failfast.describe
import org.pitest.testapi.Description
import org.pitest.testapi.ResultCollector
import org.pitest.testapi.TestUnitFinder
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object FailFastTestUnitFinderTest {
    val context = describe(FailFastTestUnitFinder::class) {
        test("finds test contexts") {
            val finder: TestUnitFinder = FailFastTestUnitFinder
            val testUnit: FailFastTestUnitFinder.FailFastTestUnit =
                finder.findTestUnits(FailFastTestPluginFactoryTest::class.java)
                    .single() as FailFastTestUnitFinder.FailFastTestUnit
            val context = FailFastTestPluginFactoryTest.context
            expectThat(testUnit.context).isEqualTo(context)
            expectThat(testUnit.description.name).isEqualTo(context.name)
            testUnit.execute(TestResultCollector())
        }
    }
}

class TestResultCollector : ResultCollector {
    override fun notifyEnd(description: Description?, t: Throwable?) {
    }

    override fun notifyEnd(description: Description?) {
    }

    override fun notifyStart(description: Description?) {
    }

    override fun notifySkipped(description: Description?) {
    }

    override fun shouldExit() = false

}

