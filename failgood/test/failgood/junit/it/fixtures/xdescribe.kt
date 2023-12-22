package failgood.junit.it.fixtures

import failgood.describe
import failgood.dsl.ContextFunction

fun xdescribe(name: String, function: ContextFunction) = describe(name, function = function)
