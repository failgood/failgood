package failgood

@Test
class TopLevelTestsMethodsTest {
    val tests =
        testCollection("The testCollection top level method") {
            it("creates a context named '<className>' when called with a class") {
                val collection = testCollection(String::class) {}
                assert(collection.rootContext.name == "String")
            }
        }
}
