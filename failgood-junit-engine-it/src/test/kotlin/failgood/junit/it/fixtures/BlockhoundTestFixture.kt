package failgood.junit.it.fixtures

import failgood.Test
import failgood.describe
import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

@Test
class BlockhoundTestFixture {
    init {
        BlockHound.install(StriktIntegration())
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

class StriktIntegration : BlockHoundIntegration {
    override fun applyTo(builder: BlockHound.Builder) {
        builder.allowBlockingCallsInside("filepeek.FilePeek", "getCallerFileInfo")
    }

}
