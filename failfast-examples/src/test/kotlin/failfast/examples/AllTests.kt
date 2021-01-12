package failfast.examples

import failfast.FailFast.findTestClasses
import failfast.Suite

fun main() {
    Suite.fromClasses(findTestClasses()).run().check()
}
