package failgood.junit

import failgood.Context
import failgood.SourceInfo
import failgood.Success
import failgood.Test
import failgood.TestDescription
import failgood.TestPlusResult
import failgood.describe
import failgood.internal.ContextInfo
import kotlinx.coroutines.CompletableDeferred
import org.junit.platform.engine.UniqueId
import strikt.api.expectThat
import strikt.assertions.filter
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class CreateResponseTest {
    val context = describe(::createResponse.name) {
        val sourceInfo = SourceInfo("package.ClassName", "file", 100)
        val rootContext = Context("root context name", null, sourceInfo)
        describe("contexts") {
            val rootContextDescriptor = createResponse(
                UniqueId.forEngine("failgood"),
                listOf(ContextInfo(listOf(rootContext, Context("sub context name", rootContext)), mapOf(), setOf())),
                JunitExecutionListener()
            )
            it("creates friendly uniqueid for a root context") {
                expectThat(rootContextDescriptor.children).single().get { uniqueId.toString() }
                    .isEqualTo("[engine:failgood]/[class:root context name(package.ClassName)]")
            }
            it("creates friendly uniqueid for a sub context") {
                expectThat(rootContextDescriptor.children).single().get { children }.single()
                    .get { uniqueId.toString() }
                    .isEqualTo("[engine:failgood]/[class:root context name(package.ClassName)]/[class:sub context name]")
            }
        }
        it("creates friendly uuids for tests") {
            val test = TestDescription(rootContext, "test", sourceInfo)
            val rootContextDescriptor = createResponse(
                UniqueId.forEngine("failgood"),
                listOf(
                    ContextInfo(
                        listOf(rootContext, Context("sub context name", rootContext)),
                        mapOf(test to CompletableDeferred(TestPlusResult(test, Success(10)))),
                        setOf()
                    )
                ),
                JunitExecutionListener()
            )
            expectThat(rootContextDescriptor.children).single().get { children }.filter { it.isTest }.single()
                .get { uniqueId.toString() }
                .isEqualTo("[engine:failgood]/[class:root context name(package.ClassName)]/[method:test]")

        }
    }
}
