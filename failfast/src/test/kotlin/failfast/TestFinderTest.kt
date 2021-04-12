package failfast

import failfast.docs.ObjectMultipleContextsTest
import failfast.docs.ObjectTestContextTest
import failfast.docs.TestContextExampleTest
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context =
        describe("test finder") {
            it("can find Test classes") {
                expectThat(FailFast.findTestClasses(classIncludeRegex = Regex(".*docs.*Test.class\$")))
                    .containsExactlyInAnyOrder(
                        ObjectTestContextTest::class,
                        TestFinderTest::class.java.classLoader.loadClass("failfast.docs.TestContextOnTopLevelTest").kotlin,
                        ObjectMultipleContextsTest::class,
                        TestContextExampleTest::class
                    )
            }
        }
}
