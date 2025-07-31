import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import failgood.Test
import failgood.testCollection
import java.io.File

@Test
class PitestParserTest {
    val tests = testCollection {
        it("parses the pitest mutations.xml file") {
            val xmlMapper =
                XmlMapper().apply {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }

            val mutationsFile = File("testResources/mutations.xml")
            assert(mutationsFile.exists()) {
                "mutations.xml file should exist at ${mutationsFile.absolutePath}"
            }

            val mutationsReport = xmlMapper.readValue(mutationsFile, MutationsReport::class.java)

            assert(mutationsReport.mutations.isNotEmpty()) { "Should have parsed some mutations" }
            println("Parsed ${mutationsReport.mutations.size} mutations")

            val firstMutation = mutationsReport.mutations.first()
            assert(firstMutation.sourceFile.isNotEmpty())
            assert(firstMutation.mutatedClass.isNotEmpty())
            assert(firstMutation.lineNumber > 0)
        }

        it("calculates mutation coverage summary") {
            val xmlMapper =
                XmlMapper().apply {
                    registerKotlinModule()
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }

            val mutationsFile = File("testResources/mutations.xml")
            val mutationsReport = xmlMapper.readValue(mutationsFile, MutationsReport::class.java)

            val summary = calculateSummary(mutationsReport)

            println("\nMutation Testing Summary:")
            println("========================")
            println("Total mutations: ${summary.totalMutations}")
            println("Killed mutations: ${summary.killedMutations}")
            println("Survived mutations: ${summary.survivedMutations}")
            println("No coverage mutations: ${summary.noCoverageMutations}")
            println("Timed out mutations: ${summary.timedOutMutations}")
            println("Covered mutations: ${summary.coveredMutations}")
            println("Mutation coverage: ${"%.2f".format(summary.mutationCoveragePercentage)}%")
            println("Test strength: ${"%.2f".format(summary.testStrengthPercentage)}%")

            println("\nMutations by status:")
            summary.mutationsByStatus.forEach { (status, count) -> println("  $status: $count") }

            println("\nTop 10 classes by mutation count:")
            summary.mutationsByClass
                .toList()
                .sortedByDescending { it.second.totalMutations }
                .take(10)
                .forEach { (className, stats) ->
                    println(
                        "  ${className.substringAfterLast('.')}: ${stats.totalMutations} mutations, ${"%.2f".format(stats.coveragePercentage)}% killed")
                }

            println("\nMutation coverage by file:")
            summary.mutationsByFile
                .toList()
                .sortedBy { it.first }
                .forEach { (fileName, stats) ->
                    println(
                        "  $fileName: ${stats.totalMutations} mutations, ${"%.2f".format(stats.coveragePercentage)}% killed (${stats.killedMutations} killed, ${stats.survivedMutations} survived, ${stats.noCoverageMutations} no coverage, ${stats.timedOutMutations} timed out)")
                }

            assert(summary.totalMutations > 0)
            assert(summary.mutationCoveragePercentage in 0.0..100.0)
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
