package failgood

@Test
class ContextTest {
    val tests =
        testCollection("a test context") {
            it("can tell its name with path") {
                val root = Context("root", null)
                val context = Context("name", root)
                assert(context.stringPath() == "root > name")
            }
            it("can be created from a path") {
                val path = listOf("Root", "subcontext", "subsubContext")
                val context = Context.fromPath(path)
                assert(context.path == path)
            }
            it("parents is ordered") {
                val path = listOf("Root", "subcontext", "subsubContext")
                val context = Context.fromPath(path)
                assert(context.parents.map { it.name } == path.dropLast(1))
            }
        }
}
