package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe
import reactor.blockhound.BlockHound

@Test
class BlockhoundTestFixture {
    init {
        BlockHound.install()
    }
    val context = describe("interop with blockhound") {
        describe("context that blocks") {
            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(10)
        }
        describe("other context") {
            test("with test") {}
        }
    }
}
