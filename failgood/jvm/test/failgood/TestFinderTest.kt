package failgood

import failgood.docs.*
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

@Test
class TestFinderTest {
    val context =
        describe("test finder") {
            it("can find Test classes") {
                val cl = TestFinderTest::class.java.classLoader
                val topLevelClass = cl.loadClass(testContextsOnTopLevelExampleClassName).kotlin
                expectThat(FailGood.findTestClasses(classIncludeRegex = Regex(".*docs.*.class\$")))
                    .containsExactlyInAnyOrder(
                        ClassTestContextExample::class,
                        ObjectTestContextExample::class,
                        topLevelClass,
                        ContextListExample::class,
                        TestContextExample::class
                    )
            }
        }
}
