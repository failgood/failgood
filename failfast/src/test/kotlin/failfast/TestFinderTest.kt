package failfast

import failfast.docs.ClassTestContextTest
import failfast.docs.ObjectMultipleContextsTest
import failfast.docs.ObjectTestContextTest
import failfast.docs.TestContextExampleTest
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

@Testable
class TestFinderTest {
    val context =
        describe("test finder") {
            it("can find Test classes") {
                expectThat(FailFast.findTestClasses(classIncludeRegex = Regex(".*docs.*Test.class\$")))
                    .containsExactlyInAnyOrder(
                        ClassTestContextTest::class,
                        ObjectTestContextTest::class,
                        TestFinderTest::class.java.classLoader.loadClass("failfast.docs.TestContextOnTopLevelTest").kotlin,
                        ObjectMultipleContextsTest::class,
                        TestContextExampleTest::class
                    )
            }
        }
}
