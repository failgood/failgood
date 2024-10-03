@file:Suppress("unused")

package failgood.experiments.builder

import failgood.SourceInfo
import failgood.callerSourceInfo
import failgood.dsl.ContextDSL
import failgood.internal.Path

/*
 * just playing around with a builder like syntax for contexts and tests.
 */
class BuilderTest {
    val context0 =
        describe("dsl with tag support").tagged("only").withoutIsolation {
            test("test without name") {}
        }
    val context1 =
        describe("also dsl with tag support").withoutIsolation { test("test without name") {} }
    val context2 = describe("dsl with tag support").tagged("only") { test("test without name") {} }

    private fun describe(contextName: String): ContextBuilder {
        return ContextBuilder(contextName)
    }
}

data class ContextBuilder(
    val contextName: String,
    val disabled: Boolean = false,
    val order: Int = 0,
    val isolation: Boolean = true,
    val tags: List<String> = listOf()
) {
    fun tagged(vararg s: String, function: suspend XContextDSL.() -> Unit): RootContext {
        return RootContext(
            contextName, disabled, order, isolation, tags = s.asList(), function = function)
    }

    fun withoutIsolation(function: suspend XContextDSL.() -> Unit) =
        RootContext(contextName, disabled, order, isolation, tags = tags, function = function)

    fun tagged(vararg s: String) = this.copy(tags = s.asList())
}

interface XContextDSL : ContextDSL<Unit>

data class RootContext(
    val name: String = "root",
    val disabled: Boolean = false,
    val order: Int = 0,
    val isolation: Boolean = true,
    val sourceInfo: SourceInfo = callerSourceInfo(),
    val tags: List<String>,
    val function: suspend XContextDSL.() -> Unit
) : Path {
    override val path: List<String>
        get() = listOf(name)
}
