package failfast.pitest

import failfast.describe
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
            expectThat(testUnit.context).isEqualTo(FailFastTestPluginFactoryTest.context)
        }
    }
}

