package failgood.junit

internal object FailGoodJunitTestEngineConstants {
    const val ID = "failgood"
    const val DISPLAY_NAME = "FailGood"
    const val CONFIG_KEY_DEBUG = "failgood.debug"
    const val CONFIG_KEY_NEW_JUNIT = "failgood.new.junit"
    const val CONFIG_KEY_SILENT = "failgood.silent"
    const val CONFIG_KEY_REPEAT = "failgood.repeat"
    const val CONFIG_KEY_PARALLELISM = "failgood.parallelism"

    // the filename for the debug txt file that we write when CONFIG_KEY_DEBUG is set or an error
    // occurs
    internal const val DEBUG_TXT_FILENAME = "failgood.debug.txt"

    // this config setting is only used by FailGood's own test suite to trigger execution of test
    // fixtures
    const val CONFIG_KEY_RUN_TEST_FIXTURES = "failgood.internal.runTestFixtures"
}
