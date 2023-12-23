package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.internal.TestFixture
import failgood.testsAbout

@TestFixture
object IgnoredTestFixture {
    val tests =
        testsAbout("root context") {
            it("pending test", ignored = Ignored.Because("ignore-reason")) {}
        }
}
