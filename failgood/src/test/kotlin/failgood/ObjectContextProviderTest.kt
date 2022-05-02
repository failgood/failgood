package failgood

import failgood.docs.ClassTestContextTest
import failgood.docs.ObjectMultipleContextsTest
import strikt.api.expectThat
import strikt.assertions.*
import kotlin.test.assertNotNull

@Test
class ObjectContextProviderTest {
    val context = describe(ObjectContextProvider::class) {
        it("provides a context from an class in a kotlin class (MyTest::class.java)") {
            expectThat(ObjectContextProvider(ClassTestContextTest::class).getContexts()).map { it.name }
                .containsExactlyInAnyOrder(
                    "test context defined in a kotlin class",
                    "another test context defined in a kotlin class",
                    "a test context returned by a function"
                )
        }
        it("provides a context from an object in a java class (MyTest::class.java)") {
            expectThat(ObjectContextProvider(TestFinderTest::class.java).getContexts()).single().isA<RootContext>()
                .and { get(RootContext::name).isEqualTo("test finder") }
        }
        it("provides a context from an object in a kotlin class (MyTest::class)") {
            expectThat(ObjectContextProvider(TestFinderTest::class).getContexts()).single().isA<RootContext>()
                .and { get(RootContext::name).isEqualTo("test finder") }
        }
        it("provides a list of contexts from an object in a kotlin class (MyTest::class)") {
            expectThat(ObjectContextProvider(ObjectMultipleContextsTest::class).getContexts()).hasSize(2).all {
                isA<RootContext>()
            }
        }
        it("provides a top level context from a kotlin class") {
            val classLoader = ObjectContextProviderTest::class.java.classLoader
            val clazz = classLoader.loadClass("failgood.docs.TestContextOnTopLevelTestKt")
            expectThat(ObjectContextProvider(clazz.kotlin).getContexts()).single().isA<RootContext>()
                .and { get(RootContext::name).isEqualTo("test context declared on top level") }
        }
        describe("correcting source info") {
            it("corrects the root context source info if its not coming from the loaded class") {
                val contexts =
                    ObjectContextProvider(TestClassThatUsesUtilityMethodToCreateTestContexts::class).getContexts()
                expectThat(contexts).hasSize(2).all {
                    get { sourceInfo } and {
                        get { className }.isEqualTo(
                            TestClassThatUsesUtilityMethodToCreateTestContexts::class.qualifiedName
                        )
                        get { lineNumber }.isEqualTo(1) // junit engine does not like line number 0
                    }
                }
            }
            it("does not touch the source info if it comes from the loaded class") {
                val contexts = ObjectContextProvider(OrdinaryTestClass::class).getContexts()
                expectThat(contexts).hasSize(2).all {
                    get { sourceInfo }.and {
                        get { className }.isEqualTo(OrdinaryTestClass::class.qualifiedName)
                        get { lineNumber }.isNotEqualTo(0)
                    }
                }
            }
        }
        describe("Error handling") {
            it("throws when a class contains no contexts") {
                expectThat(
                    kotlin.runCatching {
                        ObjectContextProvider(ContainsNoTests::class.java).getContexts()
                    }
                ).isFailure().isA<ErrorLoadingContextsFromClass>().and {
                    message.isEqualTo("no contexts found in class")
                    get { cause }.isNull()
                    get { jClass }.isEqualTo(ContainsNoTests::class.java)
                }
            }
            listOf(ClassThatThrowsAtCreationTime::class, ClassThatThrowsAtContextGetter::class).forEach { kClass1 ->
                it("wraps exceptions that happen at class instantiation: ${kClass1.simpleName}") {
                    expectThat(
                        kotlin.runCatching {
                            ObjectContextProvider(kClass1).getContexts()
                        }
                    ).isFailure().isA<ErrorLoadingContextsFromClass>().and {
                        message.isEqualTo("Could not load contexts from class")
                        get { cause }.isNotNull().message.isEqualTo("boo i failed")
                        get { jClass }.isEqualTo(kClass1.java)
                    }
                }
            }
            it("gives a helpful error when a class could not be instantiated") {
                val kClass = ClassWithConstructorParameter::class
                val exception = assertNotNull(
                    kotlin.runCatching {
                        ObjectContextProvider(kClass).getContexts()
                    }.exceptionOrNull()
                )
                val message = assertNotNull(exception.message)
                assert(message.contains(kClass.simpleName!!)) { exception.stackTraceToString() }
            }
            // this is maybe no longer needed because test containers have an annotation now
            it("ignores private classes") {
                val jClass = javaClass.classLoader.loadClass("failgood.problematic.PrivateClass")
                assert(ObjectContextProvider(jClass).getContexts() == listOf<RootContext>())
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    class ClassWithConstructorParameter(s: String)

    class ClassThatThrowsAtCreationTime {
        init {
            throw RuntimeException("boo i failed")
        }
    }
    class ClassThatThrowsAtContextGetter {
        val context: RootContext = throw RuntimeException("boo i failed")
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

class ContainsNoTests
