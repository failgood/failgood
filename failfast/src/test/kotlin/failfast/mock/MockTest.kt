package failfast.mock

import failfast.FailFast
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailFast.runTest()
}

// I have no idea how to support overloaded methods with that syntax, so it will probably change.
object MockTest {
    interface IImpl {
        fun method()
        fun methodWithParameters(number: Int, name: String)
        suspend fun suspendMethod(number: Int, name: String)
    }

    val context = describe("the mocking framework") {
        it("records method calls") {
            val mock = mock<IImpl>()
            mock.method()
            expectThat(getCalls(mock)).isEqualTo(listOf(MethodCall(IImpl::method, listOf())))
        }
        it("records method parameters") {
            val mock = mock<IImpl>()
            mock.methodWithParameters(10, "string")
            expectThat(getCalls(mock)).isEqualTo(listOf(MethodCall(IImpl::methodWithParameters, listOf(10, "string"))))
        }
        it("records suspend method calls") {
            val mock = mock<IImpl>()
            mock.suspendMethod(10, "string")
            expectThat(getCalls(mock)).isEqualTo(listOf(MethodCall(IImpl::suspendMethod, listOf(10, "string"))))
        }
    }

}

