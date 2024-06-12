package failgood.experiments

import failgood.FailGood
import failgood.Ignored
import failgood.Test
import failgood.dsl.TestInfo
import failgood.testsAbout

@Test
object TestInfoTest {
    val tests = testsAbout("accessing testInfo") {
        it(
            "is possible for methods called from tests",
            ignored = Ignored.Because("I have no idea how to implement this")
        ) {
            assert(functionThatIsCalledFromTest() == this.testInfo)
        }
    }

    private fun functionThatIsCalledFromTest(): TestInfo {
        return FailGood.currentTest()
    }
}

private fun FailGood.currentTest(): TestInfo {
    TODO("Not yet implemented")
}
