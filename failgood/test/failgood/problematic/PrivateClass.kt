@file:Suppress("unused")

package failgood.problematic

import failgood.describe
import failgood.tests

private class PrivateClass {
    val context = tests("root") {}
}
