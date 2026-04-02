import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "mutations")
data class MutationsReport(
    @param:JacksonXmlProperty(isAttribute = true) val partial: Boolean = false,
    @param:JacksonXmlProperty(localName = "mutation")
    @param:JacksonXmlElementWrapper(useWrapping = false)
    val mutations: List<Mutation> = emptyList()
)

data class Mutation(
    @param:JacksonXmlProperty(isAttribute = true) val detected: Boolean,
    @param:JacksonXmlProperty(isAttribute = true) val status: String,
    @param:JacksonXmlProperty(isAttribute = true) val numberOfTestsRun: Int,
    val sourceFile: String,
    val mutatedClass: String,
    val mutatedMethod: String,
    val methodDescription: String,
    val lineNumber: Int,
    val mutator: String,
    val description: String,
    val killingTest: String? = null,
    val indexes: Indexes? = null,
    val blocks: Blocks? = null
)

data class Indexes(
    @param:JacksonXmlProperty(localName = "index")
    @param:JacksonXmlElementWrapper(useWrapping = false)
    val index: List<Int> = emptyList()
)

data class Blocks(
    @param:JacksonXmlProperty(localName = "block")
    @param:JacksonXmlElementWrapper(useWrapping = false)
    val block: List<Int> = emptyList()
)

data class MutationSummary(
    val totalMutations: Int,
    val killedMutations: Int,
    val survivedMutations: Int,
    val noCoverageMutations: Int,
    val timedOutMutations: Int,
    val mutationCoveragePercentage: Double,
    val testStrengthPercentage: Double,
    val coveredMutations: Int,
    val mutationsByStatus: Map<String, Int>,
    val mutationsByClass: Map<String, ClassMutationStats>,
    val mutationsByFile: Map<String, FileMutationStats>
)

data class FileMutationStats(
    val totalMutations: Int,
    val killedMutations: Int,
    val survivedMutations: Int,
    val noCoverageMutations: Int,
    val timedOutMutations: Int,
    val coveragePercentage: Double
)

data class ClassMutationStats(
    val totalMutations: Int,
    val killedMutations: Int,
    val survivedMutations: Int,
    val coveragePercentage: Double
)
