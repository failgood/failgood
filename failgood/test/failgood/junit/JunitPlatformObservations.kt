package failgood.junit

import failgood.Test
import failgood.testsAbout
import org.junit.platform.engine.UniqueId

/*
 * Tests that do not test failgood but document behavior that could be interesting about junit go here.
 */
@Test
class JunitPlatformObservations {
    val tests =
        testsAbout("junit platform observations") {
            describe("UniqueId") {
                it("replaces + with space when parsing") {
                    val uniqueId = UniqueId.parse("[engine:failgood]/[class:My+Class]")
                    assert(uniqueId.toString() == "[engine:failgood]/[class:My Class]")
                }
            }
        }
}
