@file:Suppress("UNUSED_PARAMETER")

package failgood.exp.meta

import failgood.FailGood
import failgood.Suite

@MetaTest
object Configs {
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
