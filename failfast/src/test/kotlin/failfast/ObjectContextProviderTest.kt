package failfast

import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.single

fun main() {
    FailFast.runTest()
}

object ObjectContextProviderTest {
    val context =
        describe(ObjectContextProvider::class) {
            it("provides a context from an object in a java class (MyTest::class.java)") {
                expectThat(ObjectContextProvider(TestFinderTest::class.java).getContexts()).single()
                    .isA<RootContext>()
                    .and { get(RootContext::name).isEqualTo("test finder") }
            }
            it("provides a context from an object in a kotlin class (MyTest::class)") {
                expectThat(ObjectContextProvider(TestFinderTest::class).getContexts()).single()
                    .isA<RootContext>()
                    .and { get(RootContext::name).isEqualTo("test finder") }
            }
            it("provides a top level context from a kotlin class") {
                expectThat(ObjectContextProvider(ObjectContextProviderTest::class.java.classLoader.loadClass("failfast.docs.TestContextOnTopLevelTest").kotlin).getContexts()).single()
                    .isA<RootContext>()
                    .and { get(RootContext::name).isEqualTo("test context declared on top level") }
            }
        }
}
