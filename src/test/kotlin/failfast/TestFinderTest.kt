package failfast

import failfast.docs.ContractsTest
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
import kotlin.system.measureTimeMillis

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context =
        describe("test finder") {
            it("can find Test classes") {
                println(
                    measureTimeMillis {
                        expectThat(FailFast.findTestClasses(TestFinderTest::class))
                            .containsExactlyInAnyOrder(
                                ObjectTestContextTest::class,
                                TestFinderTest::class.java
                                    .classLoader
                                    .loadClass("failfast.docs.TestContextOnTopLevelTest").kotlin,
                                ContextExecutorTest::class,
                                ContextTest::class,
                                FailFastTest::class,
                                ExceptionPrettyPrinterTest::class,
                                //                        MultiThreadingPerformanceTestNotAlways::class,
                                RootContextTest::class,
                                SuiteTest::class,
                                TestFinderTest::class,
                                ObjectContextProviderTest::class,
                                TestLifecycleTest::class,
                                ContextTreeReporterTest::class,
                                SingleTestExecutorTest::class,
                                FailFastTestUnitFinderTest::class,
                                FailFastTestPluginFactoryTest::class,
                                ContractsTest::class,
                                Junit4ReporterTest::class
                            )
                    }
                )
            }
        }
}
