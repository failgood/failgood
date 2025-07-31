import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failgood.Test
import failgood.testCollection
import java.io.File

@Test
class CoverageReporterWorkingTest {
    val tests = testCollection {
        it("all tests pass") {
            // Test 1: Command line arguments parsing
            val args = arrayOf("--report-dir", "/custom/dir", "--git-hash", "test123")
            val parser = ArgParser(args)
            assert(parser.getString("--report-dir", "default") == "/custom/dir")
            assert(parser.getStringOrNull("--git-hash") == "test123")

            // Test 2: Basic functionality
            val tempDir = File("/tmp/coverage-test-${System.currentTimeMillis()}")
            tempDir.mkdirs()
            try {
                val reporter =
                    CoverageReporter(
                        reportDir = "testResources",
                        outputDir = tempDir.absolutePath,
                        gitHashOverride = "test-abc")
                reporter.run()

                // Verify file was created
                val expectedFile = File(tempDir, "test-abc-1.json")
                assert(expectedFile.exists())

                // Verify JSON content
                val mapper = jacksonObjectMapper()
                val report = mapper.readValue<CoverageReport>(expectedFile)
                assert(report.gitHash == "test-abc")
                assert(report.runNumber == 1)
                assert(report.overall.lineCoverage > 0)

                // Test 3: Run number increment
                val reporter2 =
                    CoverageReporter(
                        reportDir = "testResources",
                        outputDir = tempDir.absolutePath,
                        gitHashOverride = "test-abc")
                reporter2.run()

                val secondFile = File(tempDir, "test-abc-2.json")
                assert(secondFile.exists())

                // Test 4: Missing files error handling
                try {
                    val badReporter =
                        CoverageReporter(
                            reportDir = "/non/existent",
                            outputDir = tempDir.absolutePath,
                            gitHashOverride = "test")
                    badReporter.run()
                    assert(false) { "Should have thrown exception" }
                } catch (e: IllegalStateException) {
                    assert(e.message?.contains("mutations.xml not found") == true)
                }

                println("All tests passed!")
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }
}
