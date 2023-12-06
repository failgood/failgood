package failgood.junit.it.fixtures

import failgood.describe
import failgood.internal.TestFixture
import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

@TestFixture
class BlockhoundTestFixture {
    init {
        BlockHound.install(StriktIntegration())
    }

    val context =
        describe("interop with blockhound") {
            describe("context that blocks") {
                @Suppress("BlockingMethodInNonBlockingContext") Thread.sleep(10)
            }
            describe("other context") { test("with test") {} }
        }
}

class StriktIntegration : BlockHoundIntegration {
    override fun applyTo(builder: BlockHound.Builder) {
        builder.allowBlockingCallsInside("filepeek.FilePeek", "getCallerFileInfo")
    }
}
