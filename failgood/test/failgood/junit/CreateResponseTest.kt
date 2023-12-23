package failgood.junit

import failgood.*
import failgood.internal.ContextInfo
import failgood.internal.FailedRootContext
import failgood.internal.SuiteExecutionContext
import kotlinx.coroutines.CompletableDeferred
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.ClassSource
import strikt.api.expectThat
import strikt.assertions.filter
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class CreateResponseTest {
    val context =
        testsAbout(::createResponse.name) {
            val sourceInfo = SourceInfo("package.ClassName", "file", 100)
            val rootContext = Context("root context name", null, sourceInfo)
            val suiteExecutionContext = SuiteExecutionContext(1)
            describe("contexts") {
                val failGoodEngineDescriptor = ChannelExecutionListener()
                val rootContextDescriptor =
                    createResponse(
                        UniqueId.forEngine("failgood"),
                        listOf(
                            ContextInfo(
                                listOf(rootContext, Context("sub context name", rootContext)),
                                mapOf(),
                                setOf()
                            )
                        ),
                        FailGoodEngineDescriptor(
                            UniqueId.forEngine("failgood"),
                            listOf(
                                ContextInfo(
                                    listOf(rootContext, Context("sub context name", rootContext)),
                                    mapOf(),
                                    setOf()
                                )
                            ),
                            failGoodEngineDescriptor,
                            suiteExecutionContext
                        )
                    )
                it("creates friendly uniqueid for a root context") {
                    expectThat(rootContextDescriptor.children)
                        .single()
                        .get { uniqueId.toString() }
                        .isEqualTo("[engine:failgood]/[class:root context name(package.ClassName)]")
                }
                it("creates friendly uniqueid for a sub context") {
                    expectThat(rootContextDescriptor.children)
                        .single()
                        .get { children }
                        .single()
                        .get { uniqueId.toString() }
                        .isEqualTo(
                            "[engine:failgood]/[class:root context name(package.ClassName)]/[class:sub context name]"
                        )
                }
            }
            describe("failed contexts") {
                val failGoodEngineDescriptor = ChannelExecutionListener()
                val rootContextDescriptor =
                    createResponse(
                        UniqueId.forEngine("failgood"),
                        listOf(FailedRootContext(rootContext, RuntimeException())),
                        FailGoodEngineDescriptor(
                            UniqueId.forEngine("failgood"),
                            listOf(FailedRootContext(rootContext, RuntimeException())),
                            failGoodEngineDescriptor,
                            suiteExecutionContext
                        )
                    )
                it("creates a test node with friendly uniqueid for a failed root context") {
                    val children = rootContextDescriptor.children

                    expectThat(children).single().and {
                        // failed contexts must be tests or junit does not find them
                        get { type }.isEqualTo(TestDescriptor.Type.TEST)
                        // gradle needs all root contexts to have a class source
                        get { source.get() }.isA<ClassSource>()
                        get { uniqueId.toString() }
                            .isEqualTo(
                                "[engine:failgood]/[class:root context name(package.ClassName)]"
                            )
                    }
                }
            }
            it("creates friendly uuids for tests") {
                val test = TestDescription(rootContext, "test", sourceInfo)
                val failGoodEngineDescriptor = ChannelExecutionListener()
                val rootContextDescriptor =
                    createResponse(
                        UniqueId.forEngine("failgood"),
                        listOf(
                            ContextInfo(
                                listOf(rootContext, Context("sub context name", rootContext)),
                                mapOf(
                                    test to CompletableDeferred(TestPlusResult(test, Success(10)))
                                ),
                                setOf()
                            )
                        ),
                        FailGoodEngineDescriptor(
                            UniqueId.forEngine("failgood"),
                            listOf(
                                ContextInfo(
                                    listOf(rootContext, Context("sub context name", rootContext)),
                                    mapOf(
                                        test to
                                                CompletableDeferred(TestPlusResult(test, Success(10)))
                                    ),
                                    setOf()
                                )
                            ),
                            failGoodEngineDescriptor,
                            suiteExecutionContext
                        )
                    )
                expectThat(rootContextDescriptor.children)
                    .single()
                    .get { children }
                    .filter { it.isTest }
                    .single()
                    .get { uniqueId.toString() }
                    .isEqualTo(
                        "[engine:failgood]/[class:root context name(package.ClassName)]/[method:test]"
                    )
            }
        }
}
