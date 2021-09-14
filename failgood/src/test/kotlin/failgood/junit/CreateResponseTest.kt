package failgood.junit

import failgood.Context
import failgood.Test
import failgood.describe
import failgood.internal.ContextInfo
import org.junit.platform.engine.UniqueId
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.single

@Test
class CreateResponseTest {
    val context = describe(::createResponse.name) {
        val rootContext = Context("root context name")
        val rootContextDescriptor = createResponse(
            UniqueId.forEngine("failgood"),
            listOf(ContextInfo(listOf(rootContext, Context("sub context name", rootContext)), mapOf(), setOf())),
            JunitExecutionListener()
        )
        it("creates friendly uniqueid for a root context") {
            expectThat(rootContextDescriptor.children).single().get { uniqueId.toString() }
                .isEqualTo("[engine:failgood]/[class:root context name]")
        }
        it("creates friendly uniqueid for a sub context") {
            expectThat(rootContextDescriptor.children).single().get { children }.single()
                .get { uniqueId.toString() }
                .isEqualTo("[engine:failgood]/[class:root context name]/[class:sub context name]")
        }
    }
}
