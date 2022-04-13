package failgood

import failgood.docs.ClassTestContextTest
import failgood.docs.ObjectMultipleContextsTest
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
                    cl.loadClass("failgood.docs.TestContextOnTopLevelTest").kotlin
                expectThat(FailGood.findTestClasses(classIncludeRegex = Regex(".*docs.*.class\$")))
                    .containsExactlyInAnyOrder(
                        ClassTestContextTest::class,
                        ObjectTestContextTest::class,
                        topLevelClass,
                        ObjectMultipleContextsTest::class,
                        TestContextExampleTest::class
                    )
            }
        }
}
