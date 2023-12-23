package failgood

import failgood.docs.ClassTestContextExample
import failgood.docs.ContextListExample
import failgood.docs.testContextsOnTopLevelExampleClassName
import failgood.fixtures.PrivateContextFixture
import kotlin.test.assertNotNull
import strikt.api.expectThat
import strikt.assertions.*

@Test
class ObjectContextProviderTest {
    val tests =
        testsAbout(ObjectContextProvider::class) {
            it("provides a context from an class in a kotlin class (MyTest::class.java)") {
                expectThat(ObjectContextProvider(ClassTestContextExample::class).getContexts())
                    .map { it.rootContext.name }
                    .containsExactlyInAnyOrder(
                        "test context defined in a kotlin class",
                        "another test context defined in a kotlin class",
                        "a test context returned by a function"
                    )
            }
            it("provides a context from an object in a java class (MyTest::class.java)") {
                expectThat(ObjectContextProvider(TestFinderTest::class.java).getContexts())
                    .single()
                    .isA<TestCollection<Unit>>()
                    .and { get { rootContext.name }.isEqualTo("test finder") }
            }
            it("provides a context from an object in a kotlin class (MyTest::class)") {
                expectThat(ObjectContextProvider(TestFinderTest::class).getContexts())
                    .single()
                    .isA<TestCollection<Unit>>()
                    .and { get { rootContext.name }.isEqualTo("test finder") }
            }
            it("provides a list of contexts from an object in a kotlin class (MyTest::class)") {
                expectThat(ObjectContextProvider(ContextListExample::class).getContexts())
                    .hasSize(2)
                    .all { isA<TestCollection<Unit>>() }
            }
            it("provides a top level context from a kotlin class") {
                val classLoader = ObjectContextProviderTest::class.java.classLoader
                val clazz = classLoader.loadClass(testContextsOnTopLevelExampleClassName)
                expectThat(ObjectContextProvider(clazz.kotlin).getContexts())
                    .single()
                    .isA<TestCollection<Unit>>()
                    .and { get { rootContext.name }.isEqualTo("test context declared on top level") }
            }
            it("handles weird contexts defined in private vals gracefully") {
                assert(
                    ObjectContextProvider(PrivateContextFixture::class)
                        .getContexts()
                        .map { it.rootContext.name }
                        .size == 2
                )
            }

            describe("correcting source info") {
                it(
                    "corrects the root context source info if its not coming from the loaded class"
                ) {
                    val contexts =
                        ObjectContextProvider(
                            TestClassThatUsesUtilityMethodToCreateTestContexts::class
                        )
                            .getContexts()
                    expectThat(contexts).hasSize(2).all {
                        get { sourceInfo } and
                                {
                                    get { className }
                                        .isEqualTo(
                                            TestClassThatUsesUtilityMethodToCreateTestContexts::class
                                                .qualifiedName
                                        )
                                    get { lineNumber }
                                        .isEqualTo(1) // junit engine does not like line number 0
                                }
                    }
                }
                it("does not touch the source info if it comes from the loaded class") {
                    val contexts = ObjectContextProvider(OrdinaryTestClass::class).getContexts()
                    expectThat(contexts).hasSize(2).all {
                        get { sourceInfo }
                            .and {
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
                    )
                        .isFailure()
                        .isA<ErrorLoadingContextsFromClass>()
                        .and {
                            message.isEqualTo("no contexts found in class")
                            get { cause }.isNull()
                            get { kClass }.isEqualTo(ContainsNoTests::class)
                        }
                }
                listOf(ClassThatThrowsAtCreationTime::class, ClassThatThrowsAtContextGetter::class)
                    .forEach { kClass1 ->
                        it(
                            "wraps exceptions that happen at class instantiation: ${kClass1.simpleName}"
                        ) {
                            expectThat(
                                kotlin.runCatching {
                                    ObjectContextProvider(kClass1).getContexts()
                                }
                            )
                                .isFailure()
                                .isA<ErrorLoadingContextsFromClass>()
                                .and {
                                    message.isEqualTo("Could not load contexts from class")
                                    get { cause }.isNotNull().message.isEqualTo("boo i failed")
                                    get { kClass }.isEqualTo(kClass1)
                                }
                        }
                    }
                it("gives a helpful error when a class could not be instantiated") {
                    val kClass = ClassWithConstructorParameter::class
                    val exception =
                        assertNotNull(
                            kotlin
                                .runCatching { ObjectContextProvider(kClass).getContexts() }
                                .exceptionOrNull()
                        )
                    val message = assertNotNull(exception.message)
                    assert(message.contains(kClass.simpleName!!)) { exception.stackTraceToString() }
                }
                it("gives a helpful error when a context method has parameters") {
                    val kClass = ClassWithContextMethodWithParameter::class
                    val exception =
                        assertNotNull(
                            kotlin
                                .runCatching { ObjectContextProvider(kClass).getContexts() }
                                .exceptionOrNull()
                        )
                    val message = assertNotNull(exception.message)
                    assert(
                        message ==
                                "context method failgood.ObjectContextProviderTest\$ClassWithContextMethodWithParameter.context(arg0 String) takes unexpected parameters"
                    ) {
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
                                .exceptionOrNull()
                        )
                    val message = assertNotNull(exception.message)
                    assert(
                        message ==
                                "Test class failgood.problematic.PrivateClass is private. Just remove the @Test annotation if you don't want to run it, or make it public if you do."
                    ) {
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
        fun context(@Suppress("UNUSED_PARAMETER") parameter: String): TestCollection<Unit> = tests {}
    }

    class ClassThatThrowsAtContextGetter {
        val context: TestCollection<Unit> = throw RuntimeException("boo i failed")
    }
}

private class TestClassThatUsesUtilityMethodToCreateTestContexts {
    @Suppress("unused") val context = ContextTools.createContexts()
}

private object ContextTools {
    fun createContexts() = listOf(testsAbout("Anything") {}, testsAbout("Another thing") {})
}

private class OrdinaryTestClass {
    @Suppress("unused") val context = listOf(
        testsAbout("Anything") {},
        testsAbout("Another thing") {})
}

class ContainsNoTests
