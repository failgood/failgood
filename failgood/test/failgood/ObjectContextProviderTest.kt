package failgood

import failgood.docs.ClassTestContextExample
import failgood.docs.ContextListExample
import failgood.docs.testContextsOnTopLevelExampleClassName
import failgood.experiments.TestWithDependencies
import failgood.fixtures.PrivateContextFixture
import kotlin.reflect.KClass
import kotlin.test.assertNotNull
import strikt.api.expectThat
import strikt.assertions.*

@Test
object ObjectContextProviderTest {
    @Suppress("DEPRECATION")
    private fun getContexts(kClass: KClass<*>) = ObjectContextProvider(kClass).getContexts()

    @Suppress("DEPRECATION")
    private fun getContexts(kClass: Class<*>) = ObjectContextProvider(kClass).getContexts()

    val context = describe {
        it("provides a context from an class in a kotlin class (MyTest::class)") {
            expectThat(getContexts(ClassTestContextExample::class))
                .map { it.context.name }
                .containsExactlyInAnyOrder(
                    "test context defined in a kotlin class",
                    "another test context defined in a kotlin class",
                    "a test context returned by a function"
                )
        }
        it("provides a context from an object in a java class (MyTest::class.java)") {
            expectThat(getContexts(TestFinderTest::class.java)).single().isA<RootContext>().and {
                get { context.name }.isEqualTo("test finder")
            }
        }
        it("provides a context from an object in a kotlin class (MyTest::class)") {
            expectThat(getContexts(TestFinderTest::class)).single().isA<RootContext>().and {
                get { context.name }.isEqualTo("test finder")
            }
        }
        it("provides a list of contexts from an object in a kotlin class (MyTest::class)") {
            expectThat(getContexts(ContextListExample::class)).hasSize(2).all { isA<RootContext>() }
        }
        it("provides a top level context from a kotlin class") {
            val classLoader = ObjectContextProviderTest::class.java.classLoader
            val clazz = classLoader.loadClass(testContextsOnTopLevelExampleClassName)
            expectThat(getContexts(clazz.kotlin)).single().isA<RootContext>().and {
                get { context.name }.isEqualTo("test context declared on top level")
            }
        }
        it("handles weird contexts defined in private vals gracefully") {
            assert(getContexts(PrivateContextFixture::class).map { it.context.name }.size == 2)
        }

        describe("correcting source info") {
            it("corrects the root context source info if its not coming from the loaded class") {
                val contexts =
                    getContexts(TestClassThatUsesUtilityMethodToCreateTestContexts::class)
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
                val contexts = getContexts(OrdinaryTestClass::class)
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
                expectThat(kotlin.runCatching { getContexts(ContainsNoTests::class.java) })
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
                        expectThat(kotlin.runCatching { getContexts(kClass1) })
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
                    assertNotNull(kotlin.runCatching { getContexts(kClass) }.exceptionOrNull())
                val message = assertNotNull(exception.message)
                assert(message.contains(kClass.simpleName!!)) { exception.stackTraceToString() }
            }
            it("gives a helpful error when a context method has parameters") {
                val kClass = ClassWithContextMethodWithParameter::class
                val exception =
                    assertNotNull(kotlin.runCatching { getContexts(kClass) }.exceptionOrNull())
                val message = assertNotNull(exception.message)
                assert(
                    message ==
                        "context method failgood.ObjectContextProviderTest\$ClassWithContextMethodWithParameter.context(arg0 String) takes unexpected parameters"
                ) {
                    exception.stackTraceToString()
                }
            }
            it("returns useful error message for private classes") {
                val jClass = javaClass.classLoader.loadClass("failgood.problematic.PrivateClass")
                val exception =
                    assertNotNull(kotlin.runCatching { getContexts(jClass) }.exceptionOrNull())
                val message = assertNotNull(exception.message)
                assert(
                    message ==
                        "Test class failgood.problematic.PrivateClass is private. Just remove the @Test annotation if you don't want to run it, or make it public if you do."
                ) {
                    exception.stackTraceToString()
                }
            }
        }
        describe("test classes with dependencies") {
            it("can return a list of dependencies") {
                val deps =
                    ObjectContextProvider(TestWithDependencies::class.java).getContextCreators()
                assert(deps.size == 2)
                assertNotNull(
                    deps.singleOrNull {
                        it.dependencies ==
                            listOf(TestWithDependencies.MySlowDockerContainer::class.java) &&
                            it.method?.name == TestWithDependencies::tests.name
                    }
                )
                assertNotNull(
                    deps.singleOrNull {
                        it.dependencies ==
                            listOf(TestWithDependencies.MySlowKafkaContainer::class.java) &&
                            it.method?.name == TestWithDependencies::otherTests.name
                    }
                )
            }
            it("handles private contexts correctly") {
                assert(
                    ObjectContextProvider(PrivateContextFixture::class).getContextCreators().all {
                        it.dependencies.isEmpty()
                    }
                )
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
        fun context(@Suppress("UNUSED_PARAMETER") parameter: String): RootContext = describe {}
    }

    class ClassThatThrowsAtContextGetter {
        val context: RootContext = throw RuntimeException("boo i failed")
    }
}

private class TestClassThatUsesUtilityMethodToCreateTestContexts {
    @Suppress("unused") val context = ContextTools.createContexts()
}

private object ContextTools {
    fun createContexts() = listOf(describe("Anything") {}, describe("Another thing") {})
}

private class OrdinaryTestClass {
    @Suppress("unused") val context = listOf(describe("Anything") {}, describe("Another thing") {})
}

class ContainsNoTests
