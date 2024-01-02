package failgood.functional

import failgood.Ignored
import failgood.Suite
import failgood.Test
import failgood.TestCollection
import failgood.assert.containsExactlyInAnyOrder
import failgood.tests
import failgood.testsAbout
import kotlin.test.assertEquals

@Test
object NameHandlingTest {
    val tests = testsAbout("name handling") {
        describe("adding test class name to test collection name") {
            it("adds the test class name to the test collection name when the test collection has no name") {
                val unnamedCollection1 = tests {}
                val unnamedCollection2 = tests {}
                val results = Suite(
                    listOf(
                        unnamedCollection1.withNewClassName("className1"),
                        unnamedCollection2.withNewClassName("className2")
                    )
                ).run(silent = true)
                assert(results.contexts.map { it.name }.containsExactlyInAnyOrder("className1", "className2"))
                assert(results.contexts.map { it.displayName }.containsExactlyInAnyOrder("className1", "className2"))
            }
        }
        describe("duplicate test collection names") {
            it("makes duplicate test collection names unique") {
                val results = Suite(
                    listOf(testsAbout("duplicate name") {}.withNewClassName("test"),
                        testsAbout("duplicate name") {}.withNewClassName("test"))
                ).run(silent = true)
                assertEquals(listOf("duplicate name", "duplicate name-1"), results.contexts.map { it.name })
                assertEquals(listOf("test: duplicate name", "test: duplicate name-1"), results.contexts.map { it.displayName })
            }
        }

    }
    private fun TestCollection<*>.withNewClassName(className: String): TestCollection<*> {
        return this.copy(
            rootContext = rootContext.copy(
                sourceInfo = rootContext.sourceInfo!!.copy(
                    className = className
                )
            )
        )
    }

}
