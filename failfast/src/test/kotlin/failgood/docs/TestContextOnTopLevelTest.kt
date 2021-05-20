@file:JvmName("TestContextOnTopLevelTest")

package failgood.docs

import failgood.describe

val context =
    describe("test context declared on top level") {
        it("is also a nice way to define your test context") {}
    }
