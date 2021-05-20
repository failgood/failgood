package failgood.pitest

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import org.pitest.testapi.TestPluginFactory
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@Testable
class FailFastTestPluginFactoryTest {
    val context =
        describe(FailFastTestPluginFactory::class) {
            it("provides description and name") {
                val factory: TestPluginFactory = FailFastTestPluginFactory()
                expectThat(factory) {
                    get { description() }.isEqualTo("fail-fast pitest plugin")
                    get { name() }.isEqualTo("failgood")
                }
            }
            describe("test framework configuration") {
                val config =
                    FailFastTestPluginFactory()
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
                    expectThat(config.testUnitFinder()).isEqualTo(FailFastTestUnitFinder)
                }
            }
        }
}
