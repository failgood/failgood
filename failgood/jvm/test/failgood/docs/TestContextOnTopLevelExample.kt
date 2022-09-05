@file:Test
package failgood.docs

import failgood.Test
import failgood.describe

// this is just needed for unit tests that want to load this file
val testContextsOnTopLevelExampleClassName: String = Throwable().stackTrace.first().className

val context =
    describe("test context declared on top level") {
        it("is also a nice way to define your test context") {}
    }
