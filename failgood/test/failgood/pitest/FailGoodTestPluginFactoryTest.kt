package failgood.pitest

import failgood.Test
import failgood.describe
import failgood.testsAbout
import org.pitest.testapi.TestPluginFactory
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@Test
class FailGoodTestPluginFactoryTest {
    val context =
        testsAbout(FailGoodTestPluginFactory::class) {
            it("provides description and name") {
                val factory: TestPluginFactory = FailGoodTestPluginFactory()
                expectThat(factory) {
                    get { description() }.isEqualTo("FailGood pitest plugin")
                    get { name() }.isEqualTo("failgood")
                }
            }
            describe("test framework configuration") {
                val config =
                    FailGoodTestPluginFactory()
                        .createTestFrameworkConfiguration(
                            null,
                            null,
                            mutableListOf(),
                            mutableListOf()
                        )

                it("returns a suite object that returns no suites") {
                    expectThat(config.testSuiteFinder().apply(this::class.java)).isEmpty()
                }
                it("returns the TestUnitFinder") {
                    expectThat(config.testUnitFinder()).isEqualTo(FailGoodTestUnitFinder)
                }
            }
        }
}
