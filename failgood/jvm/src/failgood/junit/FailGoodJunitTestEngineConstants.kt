package failgood.junit

object FailGoodJunitTestEngineConstants {
    const val ID = "failgood"
    const val DISPLAY_NAME = "FailGood"
    const val CONFIG_KEY_DEBUG = "failgood.debug"

    // this config setting is only used by FailGood's own test suite to trigger execution of test
    // fixtures
    const val RUN_TEST_FIXTURES = "failgood.internal.runTestFixtures"
    const val FAILGOOD_NEW_JUNIT = "failgood.new.junit"
}
