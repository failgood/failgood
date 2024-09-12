package failgood.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.internal.tasks.testing.TestCompleteEvent
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.internal.id.LongIdGenerator
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.LauncherDiscoveryRequest
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory

class CustomTestEnginePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        project.tasks.register("customTest", CustomTestTask::class.java) { task ->
            task.group = "verification"
            task.description = "Runs tests using the Failogood test engine"

            project.plugins.withType(JavaPlugin::class.java) {
                val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
                val testSourceSet =
                    javaExtension.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
                task.classpath = testSourceSet.runtimeClasspath
                task.testClassesDirs = testSourceSet.output.classesDirs
            }
        }
    }
}

open class CustomTestTask : DefaultTask() {
    @Input
    @Optional
    @Option(
        option = "tests",
        description = "Sets the test class or method to be included, '*' is supported.")
    var testFilter: String = "*"

    private val idGenerator = LongIdGenerator()
    private lateinit var resultProcessor: TestResultProcessor

    @org.gradle.api.tasks.Classpath lateinit var classpath: org.gradle.api.file.FileCollection

    @org.gradle.api.tasks.InputFiles
    lateinit var testClassesDirs: org.gradle.api.file.FileCollection

    @TaskAction
    fun runTests() {
        resultProcessor = services.get(TestResultProcessor::class.java)

        val testClasses = discoverTests()
        val filteredTests = filterTests(testClasses)

        logger.lifecycle("Running custom tests with filter: $testFilter")

        executeTests(filteredTests)
    }

    private fun discoverTests(): List<DiscoverySelector> {
        val tree: FileTree =
            testClassesDirs.asFileTree.matching { filter: PatternFilterable ->
                filter.include("**/*Test.class")
            }
        return tree.files.map { file ->
            DiscoverySelectors.selectClass(
                file
                    .toRelativeString(testClassesDirs.first())
                    .removeSuffix(".class")
                    .replace('/', '.'))
        }
    }

    private fun filterTests(selectors: List<DiscoverySelector>): List<DiscoverySelector> {
        return selectors.filter { it.toString().contains(testFilter.replace("*", "")) }
    }

    private fun executeTests(selectors: List<DiscoverySelector>) {
        val request: LauncherDiscoveryRequest =
            LauncherDiscoveryRequestBuilder.request().selectors(selectors).build()

        val launcher = LauncherFactory.create()
        val listener = GradleTestExecutionListener(resultProcessor, idGenerator)

        launcher.execute(request, listener)
    }
}

class GradleTestExecutionListener(
    private val resultProcessor: TestResultProcessor,
    private val idGenerator: LongIdGenerator
) : TestExecutionListener {

    private val testIdMap = mutableMapOf<TestIdentifier, Long>()
    private var rootTestRegistered = false

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        if (!rootTestRegistered) {
            val rootId = idGenerator.generateId()
            resultProcessor.started(
                createTestDescriptor(rootId, "Failogood Tests", null),
                TestStartEvent(System.currentTimeMillis()))
            rootTestRegistered = true
        }
    }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        val parentId = null
        /*        val parentId = testIdentifier.parentId
        .map { parentTestId -> testIdMap[TestIdentifier.from(parentTestId)] }
        .orElse(null)*/
        val testId = idGenerator.generateId()
        testIdMap[testIdentifier] = testId
        resultProcessor.started(
            createTestDescriptor(testId, testIdentifier.displayName, parentId),
            TestStartEvent(System.currentTimeMillis()))
    }

    override fun executionFinished(
        testIdentifier: TestIdentifier,
        testExecutionResult: TestExecutionResult
    ) {
        val testId = testIdMap[testIdentifier] ?: return
        val result =
            when (testExecutionResult.status) {
                TestExecutionResult.Status.SUCCESSFUL -> TestResult.ResultType.SUCCESS
                TestExecutionResult.Status.FAILED -> TestResult.ResultType.FAILURE
                TestExecutionResult.Status.ABORTED -> TestResult.ResultType.SKIPPED
            }
        resultProcessor.completed(testId, TestCompleteEvent(System.currentTimeMillis(), result))
    }

    private fun createTestDescriptor(
        id: Long,
        name: String,
        parentId: Long?
    ): TestDescriptorInternal {
        return object : TestDescriptorInternal {
            override fun getId(): Any = id

            override fun getName(): String = name

            override fun getClassName(): String = name.split(".").dropLast(1).joinToString(".")

            override fun getClassDisplayName(): String = className

            override fun getParent(): TestDescriptorInternal? =
                null // We might need to implement proper parent handling

            override fun getDisplayName(): String = name

            override fun isComposite(): Boolean = !testIdMap.any { it.value == parentId }

            override fun toString(): String = name
        }
    }
}
