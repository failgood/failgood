package failgood.pitest

import failgood.Test
import failgood.testCollection
import org.pitest.testapi.TestPluginFactory

@Test
class FailGoodTestPluginFactoryTest {
    val tests =
        testCollection(FailGoodTestPluginFactory::class) {
            it("provides description and name") {
                val factory: TestPluginFactory = FailGoodTestPluginFactory()
                assert(factory.description() == "FailGood pitest plugin")
                assert(factory.name() == "failgood")
            }
            describe("test framework configuration") {
                val config =
                    FailGoodTestPluginFactory()
                        .createTestFrameworkConfiguration(
                            null, null, mutableListOf(), mutableListOf())

                it("returns a suite object that returns no suites") {
                    assert(config.testSuiteFinder().apply(this::class.java).isEmpty())
                }
                it("returns the TestUnitFinder") {
                    assert(config.testUnitFinder() == FailGoodTestUnitFinder)
                }
            }
        }
}
