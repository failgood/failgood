package failfast

import strikt.api.expectThat
import strikt.assertions.isEqualTo

object ContextTest {
    private val automaticallyNamedContext: RootContext = context {}

    val context = context {
        test("root context can get name from enclosing object") {
            expectThat(automaticallyNamedContext.name).isEqualTo(ContextTest::class.simpleName)
        }
    }
}
