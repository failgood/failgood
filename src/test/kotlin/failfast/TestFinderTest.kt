package failfast

import failfast.docs.ObjectTestContextTest
import failfast.internal.ContextExecutorTest
import failfast.internal.ExceptionPrettyPrinterTest
import failfast.internal.TestExecutorTest
import failfast.pitest.FailFastTestPluginFactoryTest
import failfast.pitest.FailFastTestUnitFinderTest
import kotlin.system.measureTimeMillis
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

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
                                ObjectTestContextTest::class.java,
                                TestFinderTest::class.java
                                    .classLoader
                                    .loadClass("failfast.docs.TestContextOnTopLevelTest"),
                                ContextExecutorTest::class.java,
                                ContextTest::class.java,
                                FailFastTest::class.java,
                                ExceptionPrettyPrinterTest::class.java,
                                //                        MultiThreadingPerformanceTestNotAlways::class.java,
                                RootContextTest::class.java,
                                SuiteTest::class.java,
                                TestFinderTest::class.java,
                                ObjectContextProviderTest::class.java,
                                TestLifecycleTest::class.java,
                                ContextTreeReporterTest::class.java,
                                TestExecutorTest::class.java,
                                FailFastTestUnitFinderTest::class.java,
                                FailFastTestPluginFactoryTest::class.java
                            )
                    }
                )
            }
        }
}
