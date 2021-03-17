package failfast.mock

import failfast.FailFast
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

object MockTest {
    interface IImpl {
        fun method()
        suspend fun suspendMethod()
    }

    val context = describe("the mocking framework") {
        it("records method calls") {
            val mock = mock<IImpl>()
            mock.method()
            expectThat(getCalls(mock)).isEqualTo(listOf(CallInfo(IImpl::method, listOf())))
        }
        it("records suspend method calls") {
            val mock = mock<IImpl>()
            mock.suspendMethod()
            expectThat(getCalls(mock)).isEqualTo(listOf(CallInfo(IImpl::suspendMethod, listOf())))
        }
    }

}

