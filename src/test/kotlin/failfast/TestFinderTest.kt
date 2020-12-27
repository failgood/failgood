package failfast

import failfast.docs.ObjectTestContextTest
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import kotlin.system.measureTimeMillis

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context = describe("test finder") {
        it("can find Test classes") {
            println(measureTimeMillis {
                expectThat(FailFast.findTestClasses(TestFinderTest::class))
                    .containsExactlyInAnyOrder(
                        ObjectTestContextTest::class.java,
                        TestFinderTest::class.java.classLoader.loadClass("failfast.docs.TestContextOnTopLevelTest"),
                        ContextExecutorTest::class.java,
                        ContextTest::class.java,
                        DescribeTest::class.java,
                        ExceptionPrettyPrinterTest::class.java,
//                        MultiThreadingPerformanceTestNotAlways::class.java,
                        RootContextTest::class.java,
                        SuiteTest::class.java,
                        TestFinderTest::class.java,
                        ObjectContextProviderTest::class.java,
                        TestLifecycleTest::class.java,
                        ContextTreeReporterTest::class.java
                    )
            })
        }
    }
}



