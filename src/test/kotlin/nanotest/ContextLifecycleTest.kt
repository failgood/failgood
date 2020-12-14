package nanotest

import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ContextLifecycleTest {
    val context = Context("Contexts") {
        test("Suite{} creates a root context") {
            expectThat(Suite {
                test("test") {}
            }.contexts.single().name).isEqualTo("root")
        }

    }

}
