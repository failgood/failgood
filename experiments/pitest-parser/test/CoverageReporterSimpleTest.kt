import failgood.Test
import failgood.testCollection
import java.io.File

@Test
class CoverageReporterSimpleTest {
    val tests = testCollection {
        it("runs without errors and produces output") {
            val tempOutputDir = File("/tmp/pitest-test-${System.currentTimeMillis()}")
            tempOutputDir.mkdirs()

            try {
                val reporter =
                    CoverageReporter(
                        reportDir = "testResources",
                        outputDir = tempOutputDir.absolutePath,
                        gitHashOverride = "test-simple")

                // Capture both stdout and stderr
                val outputStream = java.io.ByteArrayOutputStream()
                val errorStream = java.io.ByteArrayOutputStream()
                val originalOut = System.out
                val originalErr = System.err
                System.setOut(java.io.PrintStream(outputStream))
                System.setErr(java.io.PrintStream(errorStream))

                try {
                    reporter.run()
                } catch (e: Exception) {
                    println("Exception: ${e.message}")
                    e.printStackTrace()
                    throw e
                } finally {
                    System.setOut(originalOut)
                    System.setErr(originalErr)
                }

                val output = outputStream.toString()
                val errors = errorStream.toString()

                if (errors.isNotEmpty()) {
                    println("Stderr: $errors")
                }

                println("Output: '$output'")

                assert(output.isNotEmpty()) { "Expected output but got empty string" }
                assert(output.contains("Coverage:")) { "Output should contain 'Coverage:'" }

                // Check file was created
                val files = tempOutputDir.listFiles() ?: emptyArray()
                println("Files created: ${files.map { it.name }}")
                assert(files.isNotEmpty()) { "Expected at least one file to be created" }
            } finally {
                tempOutputDir.deleteRecursively()
            }
        }
    }
}
