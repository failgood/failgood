package failfast.pitest

import org.pitest.classinfo.ClassByteArraySource
import org.pitest.help.PitHelpError
import org.pitest.testapi.Configuration
import org.pitest.testapi.TestGroupConfig
import org.pitest.testapi.TestPluginFactory
import org.pitest.testapi.TestSuiteFinder
import org.pitest.testapi.TestUnitFinder
import java.util.*

class FailFastTestPluginFactory : TestPluginFactory {
    override fun description(): String = "fail-fast pitest plugin"

    override fun createTestFrameworkConfiguration(
        config: TestGroupConfig?,
        source: ClassByteArraySource?,
        excludedRunners: MutableCollection<String>?,
        includedTestMethods: MutableCollection<String>?
    ): Configuration {
        return FailFastConfiguration()
    }

    override fun name(): String = "failfast"

}

class FailFastConfiguration : Configuration {
    override fun testUnitFinder(): TestUnitFinder = FailFastTestUnitFinder

    override fun testSuiteFinder(): TestSuiteFinder {
        return FailFastTestSuiteFinder
    }

    override fun verifyEnvironment(): Optional<PitHelpError> {
        return Optional.empty()
    }

}

object FailFastTestSuiteFinder : TestSuiteFinder {
    override fun apply(t: Class<*>?): MutableList<Class<*>> = mutableListOf()
}

