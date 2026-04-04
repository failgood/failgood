package failgood.android.smoke

import failgood.Test
import failgood.testCollection

internal object SmokeState {
    @Volatile var didRun: Boolean = false
}

@Test
class AndroidSmokeSuite {
    val tests =
        testCollection("android smoke", isolation = false) {
            it("runs a passing failgood suite on device") { SmokeState.didRun = true }
            it("proves that a test body really ran") { check(SmokeState.didRun) }
        }
}
