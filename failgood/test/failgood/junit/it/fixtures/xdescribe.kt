package failgood.junit.it.fixtures

import failgood.dsl.ContextFunction
import failgood.testsAbout

fun xdescribe(name: String, function: ContextFunction) = testsAbout(name, function = function)
