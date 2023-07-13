package failgood.junit.next

import failgood.Context
import failgood.CouldNotLoadContext
import failgood.ExecutionListener
import failgood.FailGoodException
import failgood.RootContext
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.internal.LoadResults
import failgood.internal.SuiteExecutionContext
import failgood.internal.util.StringUniquer
import failgood.junit.ContextFinder
import failgood.junit.FailGoodJunitTestEngineConstants
import failgood.junit.FailGoodTestDescriptor
import failgood.junit.appendContext
import failgood.junit.createClassSource
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestEngine
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor

class NewJunitEngine : TestEngine {
    override fun getId(): String = "failgood-new"

    override fun discover(
        discoveryRequest: EngineDiscoveryRequest,
        uniqueId: UniqueId
    ): TestDescriptor {
        val suiteExecutionContext = SuiteExecutionContext()

        val runTestFixtures =
            discoveryRequest.configurationParameters
                .getBoolean(FailGoodJunitTestEngineConstants.RUN_TEST_FIXTURES)
                .orElse(false)
        val suiteAndFilters = ContextFinder(runTestFixtures).findContexts(discoveryRequest)
        suiteAndFilters
            ?: return EngineDescriptor(uniqueId, FailGoodJunitTestEngineConstants.displayName)
        val loadResults =
            runBlocking(suiteExecutionContext.coroutineDispatcher) {
                suiteAndFilters.suite.getRootContexts(suiteExecutionContext.scope)
            }

        val uniqueMaker = StringUniquer()
        val mapper = ContextMapper()

        val descriptors =
            loadResults.loadResults.map { rootContext ->
                when (rootContext) {
                    is RootContext -> {
                        val path =
                            uniqueMaker.makeUnique(
                                "${rootContext.context.name}(${(rootContext.context.sourceInfo!!.className)})"
                            )
                        val contextUniqueId = uniqueId.appendContext(path)

                        FailGoodTestDescriptor(
                            TestDescriptor.Type.CONTAINER,
                            contextUniqueId,
                            rootContext.context.name,
                            createClassSource(rootContext.sourceInfo)
                        ).also { mapper[rootContext.context] = it }
                    }

                    is CouldNotLoadContext ->
                        FailGoodTestDescriptor(
                            TestDescriptor.Type.CONTAINER,
                            uniqueId.appendContext(rootContext.jClass.name),
                            rootContext.jClass.name,
                            null
                        )
                }
            }
        return FailGoodEngineDescriptor(uniqueId, id, loadResults, suiteExecutionContext, mapper)
            .apply { descriptors.forEach { this.addChild(it) } }
    }

    override fun execute(request: ExecutionRequest) {
        val root = request.rootTestDescriptor
        if (root !is FailGoodEngineDescriptor) return
        try {
            runBlocking(root.suiteExecutionContext.coroutineDispatcher) {
                root.loadResults.investigate(
                    root.suiteExecutionContext.scope,
                    listener = NewExecutionListener(root.mapper, request.engineExecutionListener)
                ).awaitAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal class FailGoodEngineDescriptor(
        uniqueId: UniqueId?,
        displayName: String?,
        val loadResults: LoadResults,
        val suiteExecutionContext: SuiteExecutionContext,
        val mapper: ContextMapper
    ) : EngineDescriptor(uniqueId, displayName)
}

internal class ContextMapper {
    val map = mutableMapOf<Context, FailGoodTestDescriptor>()
    operator fun set(context: Context, value: FailGoodTestDescriptor) {
        map[context] = value
    }

    operator fun get(context: Context) = map[context] ?: throw FailGoodException("no mapping for context $context")
}

internal class NewExecutionListener(
    private val mapper: ContextMapper,
    private val engineExecutionListener: EngineExecutionListener
) :
    ExecutionListener {
        override suspend fun testDiscovered(testDescription: TestDescription) {
            val failGoodTestDescriptor = mapper[testDescription.container]
            val testDescriptor = FailGoodTestDescriptor(
                TestDescriptor.Type.TEST,
                failGoodTestDescriptor.uniqueId.appendContext(testDescription.testName),
                testDescription.testName,
                null
            ).apply { setParent(failGoodTestDescriptor) }
            engineExecutionListener.dynamicTestRegistered(testDescriptor)
        }

        override suspend fun contextDiscovered(context: Context) {
            val failGoodTestDescriptor = mapper[context]

            val testDescriptor = FailGoodTestDescriptor(
                TestDescriptor.Type.CONTAINER,
                failGoodTestDescriptor.uniqueId.appendContext(context.name),
                context.name,
                null
            ).apply { setParent(failGoodTestDescriptor) }
            engineExecutionListener.dynamicTestRegistered(testDescriptor)
        }

        override suspend fun testStarted(testDescription: TestDescription) {
        }

        override suspend fun testFinished(testPlusResult: TestPlusResult) {
        }

        override suspend fun testEvent(testDescription: TestDescription, type: String, payload: String) {
        }
    }