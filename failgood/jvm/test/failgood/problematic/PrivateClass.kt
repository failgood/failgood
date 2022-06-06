@file:Suppress("unused")

package failgood.problematic

import failgood.describe

private class PrivateClass {
    val context = describe("root") {}
}
