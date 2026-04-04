package failgood

import failgood.docs.ClassTestContextExample
import java.nio.file.Files

@Test
class FailgoodBootstrapTest {
    val tests =
        testCollection("failgood bootstrap") {
            it("delegates class discovery to the configured finder") {
                var request: TestClassDiscoveryRequest? = null
                val bootstrap =
                    FailgoodBootstrap(
                        testClassFinderFactory = {
                            TestClassFinder {
                                request = it
                                mutableListOf(ClassTestContextExample::class)
                            }
                        },
                        callerResolver = { FailgoodBootstrapTest::class },
                        callerClassNameResolver = { FailgoodBootstrapTest::class.java.name },
                        autotestTimestampPath = Files.createTempFile("failgood-bootstrap", ".ts"))

                val result =
                    bootstrap.findTestClasses(classIncludeRegex = Regex(".*docs.*.class\$"))

                assert(result == mutableListOf(ClassTestContextExample::class))
                assert(request?.classIncludeRegex?.pattern == ".*docs.*.class\$")
            }

            it("creates an autotest suite from the configured finder") {
                val timestampPath = Files.createTempFile("failgood-bootstrap-suite", ".ts")
                val bootstrap =
                    FailgoodBootstrap(
                        testClassFinderFactory = {
                            TestClassFinder { mutableListOf(ClassTestContextExample::class) }
                        },
                        callerResolver = { FailgoodBootstrapTest::class },
                        callerClassNameResolver = { FailgoodBootstrapTest::class.java.name },
                        autotestTimestampPath = timestampPath)

                val suite = bootstrap.createAutoTestSuite(FailgoodBootstrapTest::class)

                assert(suite != null)
                assert(Files.exists(timestampPath))
                assert(suite!!.run(silent = true).allTests.isNotEmpty())
            }
        }
}
