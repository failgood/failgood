package failgood

import failgood.docs.ClassTestContextTest
import failgood.docs.ObjectMultipleContextsTest
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.*

fun main() {
    FailGood.runTest("The ObjectContextProvider > provides a context from an class in a kotlin class (MyTest::class.java)")
}

@Testable
class ObjectContextProviderTest {
    val context =
        describe(ObjectContextProvider::class) {
            it("provides a context from an class in a kotlin class (MyTest::class.java)") {
                expectThat(ObjectContextProvider(ClassTestContextTest::class).getContexts()).single()
                    .isA<RootContext>()
                    .and { get(RootContext::name).isEqualTo("test context defined in a kotlin class") }
            }
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
            it("provides a list of contexts from an object in a kotlin class (MyTest::class)") {
                expectThat(ObjectContextProvider(ObjectMultipleContextsTest::class).getContexts()).hasSize(2).all {
                    isA<RootContext>()
                }
            }
            it("provides a top level context from a kotlin class") {
                expectThat(ObjectContextProvider(ObjectContextProviderTest::class.java.classLoader.loadClass("failgood.docs.TestContextOnTopLevelTest").kotlin).getContexts()).single()
                    .isA<RootContext>()
                    .and { get(RootContext::name).isEqualTo("test context declared on top level") }
            }
        }
}
