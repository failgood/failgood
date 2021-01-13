package failfast

import failfast.docs.ObjectTestContextTest
import failfast.internal.ContextExecutorTest
import failfast.internal.ContextTreeReporterTest
import failfast.internal.ExceptionPrettyPrinterTest
import failfast.internal.Junit4ReporterTest
import failfast.internal.SingleTestExecutorTest
import failfast.pitest.FailFastTestPluginFactoryTest
import failfast.pitest.FailFastTestUnitFinderTest
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context =
        describe("test finder") {
            it("can find Test classes") {
                expectThat(FailFast.findTestClasses(randomTestClass = TestFinderTest::class))
                    .containsExactlyInAnyOrder(
                        ObjectTestContextTest::class,
                        TestFinderTest::class.java
                            .classLoader
                            .loadClass("failfast.docs.TestContextOnTopLevelTest").kotlin,
                        ContextExecutorTest::class,
                        ContextTest::class,
                        ExceptionPrettyPrinterTest::class,
                        //                        MultiThreadingPerformanceTestNotAlways::class,
                        SuiteTest::class,
                        TestFinderTest::class,
                        ObjectContextProviderTest::class,
                        TestLifecycleTest::class,
                        ContextTreeReporterTest::class,
                        SingleTestExecutorTest::class,
                        FailFastTestUnitFinderTest::class,
                        FailFastTestPluginFactoryTest::class,
                        Junit4ReporterTest::class
                    )
            }
        }
}
