import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.time.Instant
import kotlin.system.exitProcess
import org.jsoup.Jsoup

data class CoverageReport(
    val gitHash: String,
    val runNumber: Int,
    val timestamp: String,
    val overall: OverallCoverage,
    val files: Map<String, FileCoverageData>
)

data class OverallCoverage(
    val lineCoverage: Double,
    val mutationCoverage: Double,
    val testStrength: Double
)

data class FileCoverageData(val lineCoverage: Double, val mutationCoverage: Double)

fun main(args: Array<String>) {
    // Gradle might pass all args as a single string, so we need to split them
    val actualArgs =
        if (args.size == 1 && args[0].contains(" ")) {
            args[0].split(" ").toTypedArray()
        } else {
            args
        }

    val parser = ArgParser(actualArgs)
    val reportDir = parser.getString("--report-dir", "failgood/build/reports/pitest")
    val outputDir =
        parser.getString("--output-dir", System.getenv("BUILD_STATS_DIR") ?: "./buildStats")
    val gitHashOverride = parser.getStringOrNull("--git-hash")

    try {
        val reporter = CoverageReporter(reportDir, outputDir, gitHashOverride)
        reporter.run()
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}

class CoverageReporter(
    private val reportDir: String,
    private val outputDir: String,
    private val gitHashOverride: String? = null
) {
    private val xmlMapper =
        XmlMapper().apply {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    private val jsonMapper =
        jacksonObjectMapper().apply { enable(SerializationFeature.INDENT_OUTPUT) }

    fun run() {
        val gitHash = gitHashOverride ?: getGitHash()
        val runNumber = getNextRunNumber(gitHash)

        // Parse Pitest reports
        val mutationsFile = File(reportDir, "mutations.xml")
        val indexHtmlFile = File(reportDir, "index.html")

        if (!mutationsFile.exists()) {
            throw IllegalStateException("mutations.xml not found at: ${mutationsFile.absolutePath}")
        }
        if (!indexHtmlFile.exists()) {
            throw IllegalStateException("index.html not found at: ${indexHtmlFile.absolutePath}")
        }

        // Parse mutation data
        val mutationsReport = xmlMapper.readValue(mutationsFile, MutationsReport::class.java)
        val totalMutations = mutationsReport.mutations.size
        val killedMutations = mutationsReport.mutations.count { it.status == "KILLED" }
        val timedOutMutations = mutationsReport.mutations.count { it.status == "TIMED_OUT" }
        val detectedMutations = killedMutations + timedOutMutations
        val noCoverageMutations = mutationsReport.mutations.count { it.status == "NO_COVERAGE" }
        val coveredMutations = totalMutations - noCoverageMutations

        val mutationCoverage =
            if (totalMutations > 0) {
                (detectedMutations * 100.0 / totalMutations)
            } else 0.0

        val testStrength =
            if (coveredMutations > 0) {
                (detectedMutations * 100.0 / coveredMutations)
            } else 0.0

        // Parse line coverage from HTML
        val indexDoc = Jsoup.parse(indexHtmlFile, "UTF-8")
        val summaryRow = indexDoc.select("table").first()?.select("tbody tr")?.first()
        val lineCoverageData =
            summaryRow?.select("td")?.get(1)?.let { td ->
                val coverageText = td.select("div.coverage_legend").text()
                val (covered, total) = coverageText.split("/").map { it.toInt() }
                if (total > 0) (covered * 100.0 / total) else 0.0
            } ?: 0.0

        // Parse file-level coverage
        val fileCoverages = parseFileCoverages()

        // Create report
        val report =
            CoverageReport(
                gitHash = gitHash,
                runNumber = runNumber,
                timestamp = Instant.now().toString(),
                overall =
                    OverallCoverage(
                        lineCoverage = lineCoverageData,
                        mutationCoverage = mutationCoverage,
                        testStrength = testStrength),
                files = fileCoverages)

        // Save report
        saveReport(report)

        // Print one-line summary
        println(
            "Coverage: Line %.1f%% | Mutation %.1f%% | Test Strength %.1f%% | Total Mutations: %d Killed: %d, Timed out: %d"
                .format(
                    report.overall.lineCoverage,
                    report.overall.mutationCoverage,
                    report.overall.testStrength,
                    totalMutations,
                    killedMutations,
                    timedOutMutations))
    }

    private fun getGitHash(): String {
        val process = ProcessBuilder("git", "rev-parse", "HEAD").redirectErrorStream(true).start()

        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw IllegalStateException("Failed to get git hash: $output")
        }

        return output
    }

    private fun getNextRunNumber(gitHash: String): Int {
        val outputDirFile = File(outputDir)
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs()
        }

        val existingRuns =
            outputDirFile
                .listFiles { _, name -> name.startsWith("$gitHash-") && name.endsWith(".json") }
                ?.mapNotNull { file ->
                    val parts = file.nameWithoutExtension.split("-")
                    if (parts.size >= 2) {
                        parts.last().toIntOrNull()
                    } else null
                }
                ?.maxOrNull() ?: 0

        return existingRuns + 1
    }

    private fun parseFileCoverages(): Map<String, FileCoverageData> {
        val fileCoverages = mutableMapOf<String, FileCoverageData>()

        // Parse package directories
        val packageDirs =
            File(reportDir).listFiles { file ->
                file.isDirectory && file.name.startsWith("failgood")
            } ?: emptyArray()

        packageDirs.forEach { packageDir ->
            val packageIndexFile = File(packageDir, "index.html")
            if (packageIndexFile.exists()) {
                val packageDoc = Jsoup.parse(packageIndexFile, "UTF-8")
                val packageName = packageDir.name

                // Find the file breakdown table
                val fileTable =
                    packageDoc.select("table").firstOrNull { table ->
                        table.select("thead th").any { it.text() == "Line Coverage" }
                    }

                fileTable?.select("tbody tr")?.forEach { row ->
                    val cells = row.select("td")
                    if (cells.size >= 3) {
                        val fileName = cells[0].text()
                        val lineCoverageText = cells[1].select("div.coverage_legend").text()
                        val mutationCoverageText = cells[2].select("div.coverage_legend").text()

                        val lineCoverage = parseCoveragePercentage(lineCoverageText)
                        val mutationCoverage = parseCoveragePercentage(mutationCoverageText)

                        if (lineCoverage != null && mutationCoverage != null) {
                            fileCoverages["$packageName.$fileName"] =
                                FileCoverageData(
                                    lineCoverage = lineCoverage,
                                    mutationCoverage = mutationCoverage)
                        }
                    }
                }
            }
        }

        return fileCoverages
    }

    private fun parseCoveragePercentage(coverageText: String): Double? {
        return if (coverageText.contains("/")) {
            val (covered, total) = coverageText.split("/").map { it.toIntOrNull() ?: 0 }
            if (total > 0) (covered * 100.0 / total) else 0.0
        } else null
    }

    private fun saveReport(report: CoverageReport) {
        val outputDirFile = File(outputDir)
        outputDirFile.mkdirs()

        val outputFile = File(outputDirFile, "${report.gitHash}-${report.runNumber}.json")
        jsonMapper.writeValue(outputFile, report)
    }
}

class ArgParser(private val args: Array<String>) {
    fun getString(key: String, default: String): String {
        // First try to find exact match
        val index = args.indexOf(key)
        if (index != -1 && index + 1 < args.size) {
            return args[index + 1]
        }

        // Then try to find key=value format
        val prefix = "$key="
        val argWithValue = args.find { it.startsWith(prefix) }
        return argWithValue?.substring(prefix.length) ?: default
    }

    fun getStringOrNull(key: String): String? {
        // First try to find exact match
        val index = args.indexOf(key)
        if (index != -1 && index + 1 < args.size) {
            return args[index + 1]
        }

        // Then try to find key=value format
        val prefix = "$key="
        val argWithValue = args.find { it.startsWith(prefix) }
        return argWithValue?.substring(prefix.length)
    }
}
