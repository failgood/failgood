package failgood.junit.it.fixtures

import failgood.dsl.ContextFunction
import failgood.testCollection

fun xdescribe(name: String, function: ContextFunction) = testCollection(name, function = function)
