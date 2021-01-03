package failfast

import strikt.api.expectThat
import strikt.assertions.isEqualTo

object RootContextTest {
    private val automaticallyNamedContext: RootContext = context {}

    val context =
        context {
            test("root context can get name from enclosing object") {
                expectThat(automaticallyNamedContext.name)
                    .isEqualTo(RootContextTest::class.simpleName)
            }
        }
}
