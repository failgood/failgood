package failgood.pitest

import org.pitest.classinfo.ClassByteArraySource
import org.pitest.help.PitHelpError
import org.pitest.testapi.*
import java.util.*

class FailGoodTestPluginFactory : TestPluginFactory {
    override fun description(): String = "FailGood pitest plugin"

    override fun createTestFrameworkConfiguration(
        config: TestGroupConfig?,
        source: ClassByteArraySource?,
        excludedRunners: MutableCollection<String>?,
        includedTestMethods: MutableCollection<String>?
    ): Configuration {
        return FailGoodConfiguration()
    }

    override fun name(): String = "failgood"
}

class FailGoodConfiguration : Configuration {
    override fun testUnitFinder(): TestUnitFinder = FailGoodTestUnitFinder

    override fun testSuiteFinder(): TestSuiteFinder {
        return FailGoodTestSuiteFinder
    }

    override fun verifyEnvironment(): Optional<PitHelpError> {
        return Optional.empty()
    }
}

object FailGoodTestSuiteFinder : TestSuiteFinder {
    override fun apply(t: Class<*>?): MutableList<Class<*>> = mutableListOf()
}
