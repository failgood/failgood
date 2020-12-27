package failfast

import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

fun main() {
    Suite(ObjectContextProviderTest.context).run()
}

object ObjectContextProviderTest {
    val context = describe(ObjectContextProvider::class) {
        it("provides a context from an object") {
            expectThat(ObjectContextProvider(TestFinderTest::class.java).getContext()).isA<RootContext>().and {
                get(RootContext::name).isEqualTo("test finder")
            }
        }
    }

}

