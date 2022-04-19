package failgood.mock

import failgood.Test
import failgood.describe

@Test
class MockExample {
    val context = describe("new mock syntax") {
        it("looks like this") {
            val userManager: IImpl = mock()
            the(userManager) {}
        }
        it("and this") {
            @Suppress("UNUSED_VARIABLE") val userManager: IImpl = mock() {}
        }

    }

}

suspend fun <Mock : Any, Result> the(mock: Mock, lambda: suspend Mock.() -> Result):
        MockReplyRecorder<Result> = getHandler(mock).whenever(lambda)
