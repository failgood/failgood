package failgood

import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Test
class ContextTest {
    val tests =
        testCollection("a test context") {
            it("can tell its name with path") {
                val root = Context("root", null)
                val context = Context("name", root)
                expectThat(context.stringPath()).isEqualTo("root > name")
            }
            it("can be created from a path") {
                val path = listOf("Root", "subcontext", "subsubContext")
                val context = Context.fromPath(path)
                expectThat(context.path).isEqualTo(path)
            }
            it("parents is ordered") {
                val path = listOf("Root", "subcontext", "subsubContext")
                val context = Context.fromPath(path)
                expectThat(context.parents.map { it.name }).isEqualTo(path.dropLast(1))
            }
        }
}
