package failfast.pitest

import failfast.describe
import org.pitest.testapi.TestPluginFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object FailFastTestPluginFactoryTest {
    val context = describe(FailFastTestPluginFactory::class) {
        it("provides description and name") {
            val factory: TestPluginFactory = FailFastTestPluginFactory()
            expectThat(factory) {
                get { description() }.isEqualTo("fail-fast pitest plugin")
                get { name() }.isEqualTo("failfast")
            }
        }
        it("returns a config object") {
            val config = FailFastTestPluginFactory().createTestFrameworkConfiguration(
                null,
                null,
                mutableListOf(),
                mutableListOf()
            )
            expectThat(config.testSuiteFinder()).isEqualTo(FailFastTestSuiteFinder)
        }
    }
}


