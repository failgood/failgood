package failgood

import failgood.docs.ClassTestContextExample
import failgood.docs.ContextListExample
import failgood.docs.testContextsOnTopLevelExampleClassName
import failgood.fixtures.PrivateContextFixture
import kotlin.test.assertNotNull

@Test
class ObjectContextProviderTest {
    val tests =
        testCollection(ObjectContextProvider::class) {
            it("provides a context from an class in a kotlin class (MyTest::class.java)") {
                val contexts = ObjectContextProvider(ClassTestContextExample::class).getContexts()
                assert(
                    contexts.map { it.rootContext.name }.toSet() ==
                        setOf(
                            "test context defined in a kotlin class",
                            "another test context defined in a kotlin class",
                            "a test context returned by a function"))
            }
            it("provides a context from an object in a java class (MyTest::class.java)") {
                val contexts = ObjectContextProvider(TestFinderTest::class.java).getContexts()
                assert(contexts.size == 1)
                val context = contexts.single()
                assert(context.rootContext.name == "test finder")
            }
            it("provides a context from an object in a kotlin class (MyTest::class)") {
                val contexts = ObjectContextProvider(TestFinderTest::class).getContexts()
                assert(contexts.size == 1)
                val context = contexts.single()
                assert(context.rootContext.name == "test finder")
            }
            it("provides a list of contexts from an object in a kotlin class (MyTest::class)") {
                val contexts = ObjectContextProvider(ContextListExample::class).getContexts()
                assert(contexts.size == 2)
            }
            it("provides a top level context from a kotlin class") {
                val classLoader = ObjectContextProviderTest::class.java.classLoader
                val clazz = classLoader.loadClass(testContextsOnTopLevelExampleClassName)
                val contexts = ObjectContextProvider(clazz.kotlin).getContexts()
                assert(contexts.size == 1)
                val context = contexts.single()
                assert(context.rootContext.name == "test context declared on top level")
            }
            it("handles and ignores weird contexts defined in private vals gracefully") {
                assert(
                    ObjectContextProvider(PrivateContextFixture::class)
                        .getContexts()
                        .map { it.rootContext.name }
                        .size == 1)
            }

            describe("correcting source info") {
                it(
                    "corrects the root context source info if its not coming from the loaded class") {
                        val contexts =
                            ObjectContextProvider(
                                    TestClassThatUsesUtilityMethodToCreateTestContexts::class)
                                .getContexts()
                        assert(contexts.size == 2)
                        assert(
                            contexts.all {
                                it.sourceInfo.className ==
                                    TestClassThatUsesUtilityMethodToCreateTestContexts::class
                                        .qualifiedName &&
                                    it.sourceInfo.lineNumber ==
                                        1 // junit engine does not like line number 0
                            })
                    }
                it("does not touch the source info if it comes from the loaded class") {
                    val contexts = ObjectContextProvider(OrdinaryTestClass::class).getContexts()
                    assert(contexts.size == 2)
                    assert(
                        contexts.all {
                            it.sourceInfo.className == OrdinaryTestClass::class.qualifiedName &&
                                it.sourceInfo.lineNumber != 0
                        })
                }
            }
            describe("Error handling") {
                it("throws when a class contains no contexts") {
                    val result =
                        kotlin.runCatching {
                            ObjectContextProvider(ContainsNoTests::class.java).getContexts()
                        }
                    assert(result.isFailure)
                    val exception = result.exceptionOrNull()
                    assert(exception is ErrorLoadingContextsFromClass)
                    val errorException = exception as ErrorLoadingContextsFromClass
                    assert(errorException.message == "no contexts found in class")
                    assert(errorException.cause == null)
                    assert(errorException.kClass == ContainsNoTests::class)
                }
                listOf(ClassThatThrowsAtCreationTime::class, ClassThatThrowsAtContextGetter::class)
                    .forEach { kClass1 ->
                        it(
                            "wraps exceptions that happen at class instantiation: ${kClass1.simpleName}") {
                                val result =
                                    kotlin.runCatching {
                                        ObjectContextProvider(kClass1).getContexts()
                                    }
                                assert(result.isFailure)
                                val exception = result.exceptionOrNull()
                                assert(exception is ErrorLoadingContextsFromClass)
                                val errorException = exception as ErrorLoadingContextsFromClass
                                assert(
                                    errorException.message == "Could not load contexts from class")
                                assert(
                                    assertNotNull(errorException.cause).message == "boo i failed")
                                assert(errorException.kClass == kClass1)
                            }
                    }
                it("gives a helpful error when a class could not be instantiated") {
                    val kClass = ClassWithConstructorParameter::class
                    val exception =
                        assertNotNull(
                            kotlin
                                .runCatching { ObjectContextProvider(kClass).getContexts() }
                                .exceptionOrNull())
                    val message = assertNotNull(exception.message)
                    assert(message.contains(kClass.simpleName!!)) { exception.stackTraceToString() }
                }
                it("gives a helpful error when a context method has parameters") {
                    val kClass = ClassWithContextMethodWithParameter::class
                    val exception =
                        assertNotNull(
                            kotlin
                                .runCatching { ObjectContextProvider(kClass).getContexts() }
                                .exceptionOrNull())
                    val message = assertNotNull(exception.message)
                    assert(
                        message ==
                            "context method failgood.ObjectContextProviderTest\$ClassWithContextMethodWithParameter.context(arg0 String) takes unexpected parameters") {
                            exception.stackTraceToString()
                        }
                }
                // this is maybe no longer needed because test containers have an annotation now
                it("returns useful error message for private classes") {
                    val jClass =
                        javaClass.classLoader.loadClass("failgood.problematic.PrivateClass")
                    val exception =
                        assertNotNull(
                            kotlin
                                .runCatching { ObjectContextProvider(jClass).getContexts() }
                                .exceptionOrNull())
                    val message = assertNotNull(exception.message)
                    assert(
                        message ==
                            "Test class failgood.problematic.PrivateClass is private. Just remove the @Test annotation if you don't want to run it, or make it public if you do.") {
                            exception.stackTraceToString()
                        }
                    //
                }
            }
        }

    @Suppress("UNUSED_PARAMETER") class ClassWithConstructorParameter(s: String)

    class ClassThatThrowsAtCreationTime {
        init {
            throw RuntimeException("boo i failed")
        }
    }

    class ClassWithContextMethodWithParameter {
        fun context(@Suppress("UNUSED_PARAMETER") parameter: String): TestCollection<Unit> =
            testCollection {}
    }

    class ClassThatThrowsAtContextGetter {
        val context: TestCollection<Unit> = throw RuntimeException("boo i failed")
    }
}

private class TestClassThatUsesUtilityMethodToCreateTestContexts {
    @Suppress("unused") val context = ContextTools.createContexts()
}

private object ContextTools {
    fun createContexts() = listOf(testCollection("Anything") {}, testCollection("Another thing") {})
}

private class OrdinaryTestClass {
    @Suppress("unused")
    val context = listOf(testCollection("Anything") {}, testCollection("Another thing") {})
}

class ContainsNoTests
