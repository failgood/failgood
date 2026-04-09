package failgood

import kotlin.test.Test as KotlinTest
import kotlin.test.assertTrue
import kotlin.test.fail

private object IosBootstrapState {
    var didRun = false
}

class FailgoodIosBootstrapTest {
    @KotlinTest
    fun runsAFailgoodSuiteOnIos() {
        IosBootstrapState.didRun = false
        val bootstrap =
            FailgoodIosBootstrap(
                "ios bootstrap",
                testCollection("ios bootstrap") {
                    it("runs a passing failgood suite") { IosBootstrapState.didRun = true }
                },
            )

        val result = bootstrap.run()

        assertTrue(result.allOk)
        assertTrue(IosBootstrapState.didRun)
    }

    @KotlinTest
    fun throwsWhenAFailgoodSuiteFails() {
        val bootstrap =
            FailgoodIosBootstrap(
                "ios bootstrap",
                testCollection("ios bootstrap") { it("fails") { fail("expected failure") } },
            )

        var threw = false
        try {
            bootstrap.runAndThrow()
        } catch (_: FailGoodException) {
            threw = true
        }

        assertTrue(threw)
    }
}
