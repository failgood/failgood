package failgood

import failgood.docs.ClassTestContextExample
import failgood.docs.ContextListTest
import failgood.docs.ObjectTestContextTest
import failgood.docs.TestContextExampleTest
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

@Test
class TestFinderTest {
    val context =
        describe("test finder") {
            it("can find Test classes") {
                val cl = TestFinderTest::class.java.classLoader
                val topLevelClass =
                    cl.loadClass("failgood.docs.TestContextOnTopLevelTestKt").kotlin
                expectThat(FailGood.findTestClasses(classIncludeRegex = Regex(".*docs.*.class\$")))
                    .containsExactlyInAnyOrder(
                        ClassTestContextExample::class,
                        ObjectTestContextTest::class,
                        topLevelClass,
                        ContextListTest::class,
                        TestContextExampleTest::class
                    )
            }
        }
}
