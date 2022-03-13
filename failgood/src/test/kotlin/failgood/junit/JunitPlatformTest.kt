package failgood.junit

import failgood.Test
import failgood.describe
import org.junit.platform.engine.UniqueId
@Test
class JunitPlatformTest {
    val context = describe("junit platform observations") {
        describe("UniqueId") {
            it("replaces + with space when parsing") {
                val uniqueId = UniqueId.parse("[engine:failgood]/[class:My+Class]")
                assert(uniqueId.toString() == "[engine:failgood]/[class:My Class]")
            }
        }
    }

}
