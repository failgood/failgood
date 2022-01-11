package failgood

import failgood.docs.ClassTestContextTest
import failgood.docs.ObjectMultipleContextsTest
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.map
import strikt.assertions.single

@Test
class ObjectContextProviderTest {
    val context =
        describe(ObjectContextProvider::class) {
            it("provides a context from an class in a kotlin class (MyTest::class.java)") {
                expectThat(ObjectContextProvider(ClassTestContextTest::class).getContexts()).map { it.name }
                    .containsExactlyInAnyOrder(
                        "test context defined in a kotlin class",
                        "another test context defined in a kotlin class"
                    )
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
                val classLoader = ObjectContextProviderTest::class.java.classLoader
                val clazz =
                    classLoader.loadClass("failgood.docs.TestContextOnTopLevelTest")
                expectThat(ObjectContextProvider(clazz.kotlin).getContexts()).single()
                    .isA<RootContext>()
                    .and { get(RootContext::name).isEqualTo("test context declared on top level") }
            }
            describe("correcting source info") {
                it("corrects the root context source info if its not coming from the loaded class") {
                    val contexts =
                        ObjectContextProvider(TestClassThatUsesUtilityMethodToCreateTestContexts::class).getContexts()
                    expectThat(contexts).hasSize(2)
                        .all {
                            get { sourceInfo } and {
                                get { className }.isEqualTo(
                                    TestClassThatUsesUtilityMethodToCreateTestContexts::class.qualifiedName
                                )
                                get { lineNumber }.isEqualTo(1) // junit engine does not like line number 0
                            }
                        }
                }
                it("does not touch the source info if it comes from the loaded class") {
                    val contexts =
                        ObjectContextProvider(OrdinaryTestClass::class).getContexts()
                    expectThat(contexts).hasSize(2)
                        .all {
                            get { sourceInfo }.and {
                                get { className }.isEqualTo(OrdinaryTestClass::class.qualifiedName)
                                get { lineNumber }.isNotEqualTo(0)
                            }
                        }
                }
            }
        }
}

private class TestClassThatUsesUtilityMethodToCreateTestContexts {
    @Suppress("unused")
    val context = ContextTools.createContexts()
}

private object ContextTools {
    fun createContexts() = listOf(describe("Anything") {}, describe("Another thing") {})
}

private class OrdinaryTestClass {
    @Suppress("unused")
    val context = listOf(describe("Anything") {}, describe("Another thing") {})
}
