package failgood.junit.it.fixtures

import failgood.Ignored
import failgood.describe
import failgood.internal.TestFixture
import failgood.tests

@TestFixture
object IgnoredTestFixture {
    val context =
        tests("root context") {
            it("pending test", ignored = Ignored.Because("ignore-reason")) {}
        }
}
