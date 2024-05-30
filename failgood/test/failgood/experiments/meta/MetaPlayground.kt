package failgood.experiments.meta

import failgood.FailGood
import failgood.Suite

/*
this is just an experiment for now to start suites from idea. does not work from gradle.
 */
@MetaTest
object MetaPlayground {
    // maybe:
    // SuiteBuilder().createAutoTestSuite()
    // SuiteBuilder().addTestClasses("**Test").addTestClasses("...")
    fun runAutoTest(): Suite? = FailGood.createAutoTestSuite()

    fun runAllTests(): Suite = Suite(FailGood.findTestClasses())
}
/*
fun main() {
    Configs.runAutoTest()?.run()?.check()
}
*/
