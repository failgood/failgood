package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.describe
import failgood.internal.TestFixture

@TestFixture
object IgnoredTestFixture {
    val context = describe("root context") {
        it("pending test", ignored = Ignored.Because("ignore-reason")) {}
    }
}
