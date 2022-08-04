package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.describe

@Test
object SubContextIsolationTest {
    val tests = describe("sub context isolation") {
        describe("on a root context with default isolation (=ON)") {
            it("can turn isolation off for subcontexts") {
                Suite(
                    describe("root", isolation = true, disabled = false) {
                        withoutIsolation {
                            describe("child") {
                            }
                        }
                    }
                )
            }
        }
    }
}
