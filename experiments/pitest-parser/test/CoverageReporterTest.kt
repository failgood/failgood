import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failgood.Test
import failgood.testCollection
import java.io.File

@Test
class CoverageReporterTest {
    val tests = testCollection {
        describe("CoverageReporter") {
            it("parses command line arguments correctly") {
                val args =
                    arrayOf(
                        "--report-dir",
                        "/custom/report/dir",
                        "--output-dir",
                        "/custom/output/dir",
                        "--git-hash",
                        "abc123")
                val parser = ArgParser(args)

                assert(parser.getString("--report-dir", "default") == "/custom/report/dir")
                assert(parser.getString("--output-dir", "default") == "/custom/output/dir")
                assert(parser.getStringOrNull("--git-hash") == "abc123")
                assert(parser.getStringOrNull("--missing") == null)
                assert(parser.getString("--missing", "fallback") == "fallback")
            }

            it("generates coverage report from test data") {
                val tempOutputDir = File("/tmp/pitest-test-${System.currentTimeMillis()}")
                tempOutputDir.mkdirs()

                try {
                    val reporter =
                        CoverageReporter(
                            reportDir = "testResources",
                            outputDir = tempOutputDir.absolutePath,
                            gitHashOverride = "test-hash-123")

                    // Capture output
                    val outputStream = java.io.ByteArrayOutputStream()
                    val originalOut = System.out
                    System.setOut(java.io.PrintStream(outputStream))

                    try {
                        reporter.run()
                    } catch (e: Exception) {
                        System.setOut(originalOut)
                        println("Error during reporter.run(): ${e.message}")
                        throw e
                    } finally {
                        System.setOut(originalOut)
                    }

                    val output = outputStream.toString().trim()

                    println("Captured output: '$output'")

                    // Verify console output format
                    assert(output.contains("Coverage: Line")) {
                        "Output should contain 'Coverage: Line' but was: '$output'"
                    }
                    assert(output.contains("Mutation"))
                    assert(output.contains("Test Strength"))
                    assert(output.contains("%"))

                    // Verify file was created
                    val expectedFile = File(tempOutputDir, "test-hash-123-1.json")
                    assert(expectedFile.exists()) {
                        "Expected file ${expectedFile.absolutePath} to exist"
                    }

                    // Parse and verify JSON content
                    val mapper = jacksonObjectMapper()
                    val report = mapper.readValue<CoverageReport>(expectedFile)

                    assert(report.gitHash == "test-hash-123")
                    assert(report.runNumber == 1)
                    assert(report.overall.lineCoverage > 0)
                    assert(report.overall.mutationCoverage > 0)
                    assert(report.overall.testStrength > 0)
                    assert(report.files.isNotEmpty())
                } finally {
                    tempOutputDir.deleteRecursively()
                }
            }

            it("increments run number for same git hash") {
                val tempOutputDir = File("/tmp/pitest-test-${System.currentTimeMillis()}")
                tempOutputDir.mkdirs()

                try {
                    // First run
                    val reporter1 =
                        CoverageReporter(
                            reportDir = "testResources",
                            outputDir = tempOutputDir.absolutePath,
                            gitHashOverride = "test-hash-456")

                    try {
                        reporter1.run()
                    } catch (e: Exception) {
                        println("First run failed: ${e.message}")
                        throw e
                    }

                    // Second run
                    val reporter2 =
                        CoverageReporter(
                            reportDir = "testResources",
                            outputDir = tempOutputDir.absolutePath,
                            gitHashOverride = "test-hash-456")

                    try {
                        reporter2.run()
                    } catch (e: Exception) {
                        println("Second run failed: ${e.message}")
                        throw e
                    }

                    // Verify files
                    val files = tempOutputDir.listFiles() ?: emptyArray()
                    println("Files in directory: ${files.map { it.name }}")
                    assert(File(tempOutputDir, "test-hash-456-1.json").exists()) {
                        "First file should exist"
                    }
                    assert(File(tempOutputDir, "test-hash-456-2.json").exists()) {
                        "Second file should exist"
                    }
                } finally {
                    tempOutputDir.deleteRecursively()
                }
            }

            it("formats one-line summary correctly") {
                val tempOutputDir = File("/tmp/pitest-test-${System.currentTimeMillis()}")
                tempOutputDir.mkdirs()

                try {
                    val reporter =
                        CoverageReporter(
                            reportDir = "testResources",
                            outputDir = tempOutputDir.absolutePath,
                            gitHashOverride = "test-hash-789")

                    val outputStream = java.io.ByteArrayOutputStream()
                    val originalOut = System.out
                    System.setOut(java.io.PrintStream(outputStream))

                    var error: Exception? = null
                    try {
                        reporter.run()
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        System.setOut(originalOut)
                    }

                    if (error != null) {
                        println("Error during test: ${error.message}")
                        error.printStackTrace()
                        throw error
                    }

                    val output = outputStream.toString().trim()

                    // Output should be a single line (after trimming)
                    assert(!output.contains("\n")) {
                        "Output should be a single line after trimming"
                    }

                    // Should match the expected format (handle both . and , as decimal separator)
                    val pattern =
                        """Coverage: Line \d+[.,]\d% \| Mutation \d+[.,]\d% \| Test Strength \d+[.,]\d%"""
                            .toRegex()
                    assert(pattern.matches(output)) {
                        "Output '$output' doesn't match expected format"
                    }
                } finally {
                    tempOutputDir.deleteRecursively()
                }
            }

            it("handles missing report files gracefully") {
                val tempOutputDir = File("/tmp/pitest-test-${System.currentTimeMillis()}")
                tempOutputDir.mkdirs()

                try {
                    val reporter =
                        CoverageReporter(
                            reportDir = "/non/existent/path",
                            outputDir = tempOutputDir.absolutePath,
                            gitHashOverride = "test-hash")

                    var exception: Exception? = null
                    try {
                        reporter.run()
                    } catch (e: Exception) {
                        exception = e
                    }

                    assert(exception != null) { "Should throw exception for missing files" }
                    assert(exception is IllegalStateException)
                    assert(exception?.message?.contains("mutations.xml not found") == true)
                } finally {
                    tempOutputDir.deleteRecursively()
                }
            }
        }
    }
}
