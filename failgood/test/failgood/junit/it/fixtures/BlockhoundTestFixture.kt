package failgood.junit.it.fixtures

import failgood.internal.TestFixture
import failgood.testsAbout
import reactor.blockhound.BlockHound
import reactor.blockhound.integration.BlockHoundIntegration

@TestFixture
class BlockhoundTestFixture {
    init {
        BlockHound.install(StriktIntegration())
    }

    val tests =
        testsAbout("interop with blockhound") {
            describe("context that blocks") {
                Thread.sleep(10)
            }
            describe("other context") { test("with test") {} }
        }
}

class StriktIntegration : BlockHoundIntegration {
    override fun applyTo(builder: BlockHound.Builder) {
        builder.allowBlockingCallsInside("filepeek.FilePeek", "getCallerFileInfo")
    }
}
