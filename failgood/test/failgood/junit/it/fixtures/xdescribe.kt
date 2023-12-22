package failgood.junit.it.fixtures

import failgood.describe
import failgood.dsl.ContextFunction
import failgood.tests

fun xdescribe(name: String, function: ContextFunction) = tests(name, function = function)
