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
                expectThat(FailGood.findTestClasses(classIncludeRegex = Regex(".*docs.*Test.class\$")))
                    .containsExactlyInAnyOrder(
                        ClassTestContextTest::class,
                        ObjectTestContextTest::class,
                        TestFinderTest::class.java.classLoader.loadClass("failgood.docs.TestContextOnTopLevelTest").kotlin,
                        ObjectMultipleContextsTest::class,
                        TestContextExampleTest::class
                    )
            }
        }
}
