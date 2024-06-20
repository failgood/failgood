package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.internal.TestFixture
import failgood.testCollection

@TestFixture
object IgnoredTestFixture {
    val tests =
        testCollection("root context") {
            it("pending test", ignored = Ignored.Because("ignore-reason")) {}
        }
}
