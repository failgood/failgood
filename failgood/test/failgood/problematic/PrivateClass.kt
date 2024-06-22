@file:Suppress("unused")

package failgood.problematic

import failgood.testCollection

private class PrivateClass {
    val tests = testCollection("root") {}
}
