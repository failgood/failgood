import failgood.Test
import failgood.testCollection
import java.io.File
import org.jsoup.Jsoup

@Test
class PitestHtmlParserTest {
    val tests = testCollection {
        it("parses line coverage from pitest HTML reports") {
            val indexFile = File("testResources/index.html")
            assert(indexFile.exists()) { "index.html should exist at ${indexFile.absolutePath}" }

            val doc = Jsoup.parse(indexFile, "UTF-8")

            // Extract overall project summary
            val summaryTable = doc.select("table").first()
            val summaryRow = summaryTable?.select("tbody tr")?.first()

            val overallLineCoverage =
                summaryRow?.select("td")?.get(1)?.let { td ->
                    val coverageText = td.select("div.coverage_legend").text()
                    val (covered, total) = coverageText.split("/").map { it.toInt() }
                    LineCoverage(covered, total)
                }

            println(
                "Overall Line Coverage: ${overallLineCoverage?.percentage}% (${overallLineCoverage?.covered}/${overallLineCoverage?.total})")

            // Extract package breakdowns
            val packageTable = doc.select("table")[1]
            val packageRows = packageTable.select("tbody tr")

            val packageCoverages =
                packageRows.map { row ->
                    val cells = row.select("td")
                    val packageName = cells[0].text()
                    val lineCoverageCell = cells[2]
                    val coverageText = lineCoverageCell.select("div.coverage_legend").text()
                    val (covered, total) = coverageText.split("/").map { it.toInt() }
                    PackageCoverage(packageName, LineCoverage(covered, total))
                }

            println("\nLine Coverage by Package:")
            packageCoverages.forEach { pkg ->
                println(
                    "  ${pkg.name}: ${pkg.coverage.percentage}% (${pkg.coverage.covered}/${pkg.coverage.total})")
            }

            assert(overallLineCoverage != null)
            assert(overallLineCoverage?.percentage?.let { it in 0.0..100.0 } ?: false)
            assert(packageCoverages.isNotEmpty())
        }

        it("parses file-level line coverage from package HTML") {
            val packageIndexFile = File("testResources/failgood.internal.execution/index.html")
            assert(packageIndexFile.exists()) {
                "Package index.html should exist at ${packageIndexFile.absolutePath}"
            }

            val doc = Jsoup.parse(packageIndexFile, "UTF-8")

            // Find the breakdown table
            val fileTable =
                doc.select("table").firstOrNull { table ->
                    table.select("thead th").any { it.text() == "Line Coverage" }
                }

            val fileRows = fileTable?.select("tbody tr") ?: emptyList()

            val fileCoverages =
                fileRows.map { row ->
                    val cells = row.select("td")
                    val fileName = cells[0].text()
                    val lineCoverageCell = cells[1]
                    val coverageText = lineCoverageCell.select("div.coverage_legend").text()
                    val (covered, total) = coverageText.split("/").map { it.toInt() }
                    FileCoverage(fileName, LineCoverage(covered, total))
                }

            println("\nLine Coverage for files in failgood.internal.execution:")
            fileCoverages.forEach { file ->
                println(
                    "  ${file.name}: ${file.coverage.percentage}% (${file.coverage.covered}/${file.coverage.total})")
            }

            assert(fileCoverages.isNotEmpty())
            assert(fileCoverages.all { it.coverage.percentage in 0.0..100.0 })
        }

        it("combines XML mutation data with HTML line coverage") {
            // Parse mutations from XML
            val xmlMapper =
                com.fasterxml.jackson.dataformat.xml.XmlMapper().apply {
                    registerModule(com.fasterxml.jackson.module.kotlin.kotlinModule())
                    configure(
                        com.fasterxml.jackson.databind.DeserializationFeature
                            .FAIL_ON_UNKNOWN_PROPERTIES,
                        false)
                }

            val mutationsFile = File("testResources/mutations.xml")
            val mutationsReport = xmlMapper.readValue(mutationsFile, MutationsReport::class.java)
            val mutationSummary = calculateSummary(mutationsReport)

            // Parse line coverage from HTML
            val indexFile = File("testResources/index.html")
            val doc = Jsoup.parse(indexFile, "UTF-8")

            val summaryRow = doc.select("table").first()?.select("tbody tr")?.first()
            val overallLineCoverage =
                summaryRow?.select("td")?.get(1)?.let { td ->
                    val coverageText = td.select("div.coverage_legend").text()
                    val (covered, total) = coverageText.split("/").map { it.toInt() }
                    LineCoverage(covered, total)
                }

            // Create combined report
            println("\n=== Combined Pitest Report ===")
            println(
                "Line Coverage: ${overallLineCoverage?.percentage}% (${overallLineCoverage?.covered}/${overallLineCoverage?.total} lines)")
            println(
                "Mutation Coverage: ${"%.2f".format(mutationSummary.mutationCoveragePercentage)}% (${mutationSummary.killedMutations}/${mutationSummary.totalMutations} mutations)")
            println(
                "Test Strength: ${"%.2f".format(mutationSummary.testStrengthPercentage)}% (${mutationSummary.killedMutations}/${mutationSummary.coveredMutations} covered mutations)")

            assert(overallLineCoverage != null)
            assert(mutationSummary.totalMutations > 0)
        }
    }

    private fun calculateSummary(report: MutationsReport): MutationSummary {
        val mutations = report.mutations

        val killedMutations = mutations.count { it.status == "KILLED" }
        val survivedMutations = mutations.count { it.status == "SURVIVED" }
        val noCoverageMutations = mutations.count { it.status == "NO_COVERAGE" }
        val timedOutMutations = mutations.count { it.status == "TIMED_OUT" }

        val mutationsByStatus = mutations.groupingBy { it.status }.eachCount()

        val mutationsByClass =
            mutations
                .groupBy { it.mutatedClass }
                .mapValues { (_, classMutations) ->
                    val classKilled = classMutations.count { it.status == "KILLED" }
                    val classTotal = classMutations.size
                    ClassMutationStats(
                        totalMutations = classTotal,
                        killedMutations = classKilled,
                        survivedMutations = classMutations.count { it.status == "SURVIVED" },
                        coveragePercentage =
                            if (classTotal > 0) (classKilled * 100.0 / classTotal) else 0.0)
                }

        val mutationsByFile =
            mutations
                .groupBy { it.sourceFile }
                .mapValues { (_, fileMutations) ->
                    val fileKilled = fileMutations.count { it.status == "KILLED" }
                    val fileSurvived = fileMutations.count { it.status == "SURVIVED" }
                    val fileNoCoverage = fileMutations.count { it.status == "NO_COVERAGE" }
                    val fileTimedOut = fileMutations.count { it.status == "TIMED_OUT" }
                    val fileTotal = fileMutations.size
                    FileMutationStats(
                        totalMutations = fileTotal,
                        killedMutations = fileKilled,
                        survivedMutations = fileSurvived,
                        noCoverageMutations = fileNoCoverage,
                        timedOutMutations = fileTimedOut,
                        coveragePercentage =
                            if (fileTotal > 0) (fileKilled * 100.0 / fileTotal) else 0.0)
                }

        val totalMutations = mutations.size
        val coveredMutations = totalMutations - noCoverageMutations

        val mutationCoveragePercentage =
            if (totalMutations > 0) {
                (killedMutations * 100.0 / totalMutations)
            } else {
                0.0
            }

        val testStrengthPercentage =
            if (coveredMutations > 0) {
                (killedMutations * 100.0 / coveredMutations)
            } else {
                0.0
            }

        return MutationSummary(
            totalMutations = totalMutations,
            killedMutations = killedMutations,
            survivedMutations = survivedMutations,
            noCoverageMutations = noCoverageMutations,
            timedOutMutations = timedOutMutations,
            mutationCoveragePercentage = mutationCoveragePercentage,
            testStrengthPercentage = testStrengthPercentage,
            coveredMutations = coveredMutations,
            mutationsByStatus = mutationsByStatus,
            mutationsByClass = mutationsByClass,
            mutationsByFile = mutationsByFile)
    }
}

data class LineCoverage(val covered: Int, val total: Int) {
    val percentage: Double
        get() = if (total > 0) (covered * 100.0 / total) else 0.0
}

data class PackageCoverage(val name: String, val coverage: LineCoverage)

data class FileCoverage(val name: String, val coverage: LineCoverage)
