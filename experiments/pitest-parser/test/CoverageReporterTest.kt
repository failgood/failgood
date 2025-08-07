import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failgood.Test
import failgood.testCollection
import java.nio.file.Files
import kotlin.io.path.deleteRecursively

@Test
@OptIn(kotlin.io.path.ExperimentalPathApi::class)
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
                // Create temp directory with automatic cleanup
                val tempDir =
                    autoClose(Files.createTempDirectory("coverage-test-")) {
                        it.deleteRecursively()
                    }

                // Create directory structure
                val reportDir = tempDir.resolve("reports")
                val outputDir = tempDir.resolve("output")
                val packageDir = reportDir.resolve("failgood.internal")
                Files.createDirectories(packageDir)
                Files.createDirectories(outputDir)

                // Copy test fixtures from classpath
                val mutationsStream =
                    this::class.java.getResourceAsStream("/mutations.xml")
                        ?: error("Could not load /mutations.xml from classpath")
                mutationsStream.use { Files.copy(it, reportDir.resolve("mutations.xml")) }
                val indexStream =
                    this::class.java.getResourceAsStream("/index.html")
                        ?: error("Could not load /index.html from classpath")
                indexStream.use { Files.copy(it, reportDir.resolve("index.html")) }
                val packageIndexStream =
                    this::class.java.getResourceAsStream("/failgood.internal/index.html")
                        ?: error("Could not load /failgood.internal/index.html from classpath")
                packageIndexStream.use { Files.copy(it, packageDir.resolve("index.html")) }

                // Verify files were copied
                assert(Files.exists(reportDir.resolve("mutations.xml"))) {
                    "mutations.xml should exist at ${reportDir.resolve("mutations.xml")}"
                }
                assert(Files.exists(reportDir.resolve("index.html"))) {
                    "index.html should exist at ${reportDir.resolve("index.html")}"
                }

                val reporter =
                    CoverageReporter(
                        reportDir = reportDir.toString(),
                        outputDir = outputDir.toString(),
                        gitHashOverride = "test-hash-123")

                reporter.run()

                // Verify file was created
                val expectedFile = outputDir.resolve("test-hash-123-1.json")
                assert(Files.exists(expectedFile)) { "Expected file $expectedFile to exist" }

                // Parse and verify JSON content
                val mapper = jacksonObjectMapper()
                val report = mapper.readValue<CoverageReport>(expectedFile.toFile())

                assert(report.gitHash == "test-hash-123")
                assert(report.runNumber == 1)
                assert(report.overall.lineCoverage > 0)
                assert(report.overall.mutationCoverage > 0)
                assert(report.overall.testStrength > 0)
                assert(report.files.isNotEmpty())
            }

            it("increments run number for same git hash") {
                // Create temp directory with automatic cleanup
                val tempDir =
                    autoClose(Files.createTempDirectory("coverage-test-")) {
                        it.deleteRecursively()
                    }

                // Create directory structure
                val reportDir = tempDir.resolve("reports")
                val outputDir = tempDir.resolve("output")
                val packageDir = reportDir.resolve("failgood.internal")
                Files.createDirectories(packageDir)
                Files.createDirectories(outputDir)

                // Copy test fixtures from classpath
                val mutationsStream =
                    this::class.java.getResourceAsStream("/mutations.xml")
                        ?: error("Could not load /mutations.xml from classpath")
                mutationsStream.use { Files.copy(it, reportDir.resolve("mutations.xml")) }
                val indexStream =
                    this::class.java.getResourceAsStream("/index.html")
                        ?: error("Could not load /index.html from classpath")
                indexStream.use { Files.copy(it, reportDir.resolve("index.html")) }
                val packageIndexStream =
                    this::class.java.getResourceAsStream("/failgood.internal/index.html")
                        ?: error("Could not load /failgood.internal/index.html from classpath")
                packageIndexStream.use { Files.copy(it, packageDir.resolve("index.html")) }

                // First run
                val reporter1 =
                    CoverageReporter(
                        reportDir = reportDir.toString(),
                        outputDir = outputDir.toString(),
                        gitHashOverride = "test-hash-456")

                reporter1.run()

                // Second run
                val reporter2 =
                    CoverageReporter(
                        reportDir = reportDir.toString(),
                        outputDir = outputDir.toString(),
                        gitHashOverride = "test-hash-456")

                reporter2.run()

                // Verify files
                val files = Files.list(outputDir).use { it.toList() }
                println("Files in directory: ${files.map { it.fileName }}")
                assert(Files.exists(outputDir.resolve("test-hash-456-1.json"))) {
                    "First file should exist"
                }
                assert(Files.exists(outputDir.resolve("test-hash-456-2.json"))) {
                    "Second file should exist"
                }
            }

            it("creates JSON report with correct data") {
                // Create temp directory with automatic cleanup
                val tempDir =
                    autoClose(Files.createTempDirectory("coverage-test-")) {
                        it.deleteRecursively()
                    }

                // Create directory structure
                val reportDir = tempDir.resolve("reports")
                val outputDir = tempDir.resolve("output")
                val packageDir = reportDir.resolve("failgood.internal")
                Files.createDirectories(packageDir)
                Files.createDirectories(outputDir)

                // Copy test fixtures from classpath
                val mutationsStream =
                    this::class.java.getResourceAsStream("/mutations.xml")
                        ?: error("Could not load /mutations.xml from classpath")
                mutationsStream.use { Files.copy(it, reportDir.resolve("mutations.xml")) }
                val indexStream =
                    this::class.java.getResourceAsStream("/index.html")
                        ?: error("Could not load /index.html from classpath")
                indexStream.use { Files.copy(it, reportDir.resolve("index.html")) }
                val packageIndexStream =
                    this::class.java.getResourceAsStream("/failgood.internal/index.html")
                        ?: error("Could not load /failgood.internal/index.html from classpath")
                packageIndexStream.use { Files.copy(it, packageDir.resolve("index.html")) }

                val reporter =
                    CoverageReporter(
                        reportDir = reportDir.toString(),
                        outputDir = outputDir.toString(),
                        gitHashOverride = "test-hash-789")

                reporter.run()

                // Verify JSON file was created with correct data
                val expectedFile = outputDir.resolve("test-hash-789-1.json")
                assert(Files.exists(expectedFile)) { "Expected file $expectedFile to exist" }

                val mapper = jacksonObjectMapper()
                val report = mapper.readValue<CoverageReport>(expectedFile.toFile())

                assert(report.gitHash == "test-hash-789")
                assert(report.runNumber == 1)
                assert(report.overall.lineCoverage > 0)
                assert(report.overall.mutationCoverage > 0)
                assert(report.overall.testStrength > 0)
            }

            it("handles missing report files gracefully") {
                // Create temp directory with automatic cleanup
                val tempDir =
                    autoClose(Files.createTempDirectory("coverage-test-")) {
                        it.deleteRecursively()
                    }

                val outputDir = tempDir.resolve("output")
                Files.createDirectories(outputDir)

                val reporter =
                    CoverageReporter(
                        reportDir = "/non/existent/path",
                        outputDir = outputDir.toString(),
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
            }
        }
    }
}
