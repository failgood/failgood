import failgood.Test
import failgood.testCollection
import java.nio.file.Files
import kotlin.io.path.deleteRecursively

@Test
@OptIn(kotlin.io.path.ExperimentalPathApi::class)
class CoverageReporterDebugTest {
    val tests = testCollection {
        it("runs reporter without capturing output") {
            // Create temp directory with automatic cleanup
            val tempDir =
                autoClose(Files.createTempDirectory("coverage-test-")) { it.deleteRecursively() }

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
                    gitHashOverride = "test-debug")

            // Run without capturing output
            reporter.run()

            // Check if file was created
            val files = Files.list(outputDir).use { it.toList() }
            assert(files.isNotEmpty()) { "Expected at least one file to be created" }

            val expectedFile = outputDir.resolve("test-debug-1.json")
            assert(Files.exists(expectedFile)) { "Expected file ${expectedFile.fileName} to exist" }
        }
    }
}
