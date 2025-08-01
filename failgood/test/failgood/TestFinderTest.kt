package failgood

import failgood.docs.ClassTestContextExample
import failgood.docs.ContextListExample
import failgood.docs.ObjectTestContextExample
import failgood.docs.TestContextExample
import failgood.docs.testContextsOnTopLevelExampleClassName

@Test
class TestFinderTest {
    val tests =
        testCollection("test finder") {
            it("can find Test classes") {
                val cl = TestFinderTest::class.java.classLoader
                val topLevelClass = cl.loadClass(testContextsOnTopLevelExampleClassName).kotlin
                val result = FailGood.findTestClasses(classIncludeRegex = Regex(".*docs.*.class\$"))
                assert(
                    result.toSet() ==
                        setOf(
                            ClassTestContextExample::class,
                            ObjectTestContextExample::class,
                            topLevelClass,
                            ContextListExample::class,
                            TestContextExample::class))
            }
        }
}
