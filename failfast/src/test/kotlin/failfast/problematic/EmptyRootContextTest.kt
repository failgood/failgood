package failfast.problematic

import failfast.describe
import org.junit.platform.commons.annotation.Testable


@Testable
class EmptyRootContextTest {
    val context = describe("empty root context") {

    }
}
