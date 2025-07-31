import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import failgood.Test
import failgood.testCollection
import java.io.File
import org.jsoup.Jsoup

@Test
class ComprehensivePitestParserTest {
    val tests = testCollection {
        it("creates a comprehensive report with line coverage per file") {
            // Parse mutations from XML
            val xmlMapper =
                XmlMapper().apply {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }

            val mutationsFile = File("testResources/mutations.xml")
            val mutationsReport = xmlMapper.readValue(mutationsFile, MutationsReport::class.java)

            // Parse overall summary from main index.html
            val indexFile = File("testResources/index.html")
            val indexDoc = Jsoup.parse(indexFile, "UTF-8")

            val summaryRow = indexDoc.select("table").first()?.select("tbody tr")?.first()
            val overallLineCoverage =
                summaryRow?.select("td")?.get(1)?.let { td ->
                    val coverageText = td.select("div.coverage_legend").text()
                    val (covered, total) = coverageText.split("/").map { it.toInt() }
                    LineCoverage(covered, total)
                }

            // Parse all package directories to get file-level coverage
            val packageDirs =
                File("testResources").listFiles { file ->
                    file.isDirectory && file.name.startsWith("failgood")
                } ?: emptyArray()

            val allFileCoverages = mutableMapOf<String, FileCoverageDetails>()

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
                        val fileName = cells[0].text()
                        val lineCoverageCell = cells[1]
                        val coverageText = lineCoverageCell.select("div.coverage_legend").text()

                        if (coverageText.contains("/")) {
                            val (covered, total) = coverageText.split("/").map { it.toInt() }
                            val mutationCoverageCell = cells[2]
                            val mutationText =
                                mutationCoverageCell.select("div.coverage_legend").text()
                            val mutationParts =
                                if (mutationText.contains("/")) {
                                    mutationText.split("/").map { it.toInt() }
                                } else {
                                    listOf(0, 0)
                                }
                            val mutationsCovered = mutationParts[0]
                            val mutationsTotal = mutationParts[1]

                            allFileCoverages["$packageName.$fileName"] =
                                FileCoverageDetails(
                                    packageName = packageName,
                                    fileName = fileName,
                                    lineCoverage = LineCoverage(covered, total),
                                    mutationCoverage =
                                        MutationCoverage(mutationsCovered, mutationsTotal))
                        }
                    }
                }
            }

            // Combine with mutation data from XML
            val mutationsByFile =
                mutationsReport.mutations.groupBy { mutation ->
                    val className = mutation.mutatedClass
                    val sourceFile = mutation.sourceFile
                    "$className.kt" // Assuming Kotlin files
                }

            // Print comprehensive report
            println("\n=== Comprehensive Pitest Coverage Report ===")
            println("\nOverall Statistics:")
            println(
                "  Line Coverage: ${overallLineCoverage?.percentage?.let { "%.2f".format(it) }}% (${overallLineCoverage?.covered}/${overallLineCoverage?.total} lines)")

            val totalMutations = mutationsReport.mutations.size
            val killedMutations = mutationsReport.mutations.count { it.status == "KILLED" }
            val mutationPercentage =
                if (totalMutations > 0) (killedMutations * 100.0 / totalMutations) else 0.0
            println(
                "  Mutation Coverage: ${"%.2f".format(mutationPercentage)}% ($killedMutations/$totalMutations mutations)")

            println("\nFile-Level Coverage:")
            println("%-60s %15s %20s".format("File", "Line Coverage", "Mutation Coverage"))
            println("-".repeat(95))

            allFileCoverages.toSortedMap().forEach { (fullName, coverage) ->
                val linePercent = "%.2f".format(coverage.lineCoverage.percentage)
                val mutationPercent = "%.2f".format(coverage.mutationCoverage.percentage)
                val lineStats = "${coverage.lineCoverage.covered}/${coverage.lineCoverage.total}"
                val mutationStats =
                    "${coverage.mutationCoverage.killed}/${coverage.mutationCoverage.total}"

                println(
                    "%-60s %6s%% (%6s) %6s%% (%6s)"
                        .format(fullName, linePercent, lineStats, mutationPercent, mutationStats))
            }

            // Find files with low coverage
            println("\nFiles with Low Line Coverage (<80%):")
            allFileCoverages
                .filter { it.value.lineCoverage.percentage < 80 }
                .toSortedMap()
                .forEach { (fullName, coverage) ->
                    println("  $fullName: ${"%.2f".format(coverage.lineCoverage.percentage)}%")
                }

            println("\nFiles with Low Mutation Coverage (<60%):")
            allFileCoverages
                .filter {
                    it.value.mutationCoverage.total > 0 && it.value.mutationCoverage.percentage < 60
                }
                .toSortedMap()
                .forEach { (fullName, coverage) ->
                    println("  $fullName: ${"%.2f".format(coverage.mutationCoverage.percentage)}%")
                }

            assert(allFileCoverages.isNotEmpty())
            assert(overallLineCoverage != null)
        }
    }
}

data class FileCoverageDetails(
    val packageName: String,
    val fileName: String,
    val lineCoverage: LineCoverage,
    val mutationCoverage: MutationCoverage
)

data class MutationCoverage(val killed: Int, val total: Int) {
    val percentage: Double
        get() = if (total > 0) (killed * 100.0 / total) else 0.0
}
