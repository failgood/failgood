package failgood.functional

import failgood.Suite
import failgood.Test
import failgood.TestCollection
import failgood.assert.containsExactlyInAnyOrder
import failgood.softly.softly
import failgood.tests
import failgood.testsAbout

@Test
object NameHandlingTest {
    // this mostly happens in failgood.internal.LoadResults.fixRootName
    val tests = testsAbout("name handling") {
        describe("adding test class name to test collection name") {
            it("adds the test class name to the test collection name when the test collection has no name") {
                val unnamedCollection1 = tests {}
                val unnamedCollection2 = tests {}
                val results = Suite(
                    listOf(
                        unnamedCollection1.withClassName("className1"),
                        unnamedCollection2.withClassName("className2")
                    )
                ).run(silent = true)
                softly {
                    assert(results.contexts.map { it.name }.containsExactlyInAnyOrder("className1", "className2"))
                    assert(results.contexts.map { it.displayName }.containsExactlyInAnyOrder("className1", "className2"))

                }
            }
        }
        describe("duplicate test collection names") {
            it("makes duplicate test collection names unique") {
                val results = Suite(
                    listOf(testsAbout("duplicate name") {}.withClassName("test"),
                        testsAbout("duplicate name") {}.withClassName("test"))
                ).run(silent = true)
                softly {
                    assert(results.contexts.map { it.name }.containsExactlyInAnyOrder("duplicate name", "duplicate name-1"))
                    assert(results.contexts.map { it.displayName }.containsExactlyInAnyOrder("test: duplicate name", "test: duplicate name-1"))
                }
            }
        }

    }
    private fun TestCollection<*>.withClassName(className: String): TestCollection<*> {
        return this.copy(
            rootContext = rootContext.copy(
                sourceInfo = rootContext.sourceInfo!!.copy(
                    className = className
                )
            )
        )
    }

}
