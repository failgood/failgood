package failgood.examples

import failgood.Test
import failgood.testCollection

@Test
class ParametrizedTestExample {
    val tests = testCollection {
        describe("String#reverse") {
            listOf(Pair("otto", "otto"), Pair("racecar", "racecar")).forEach { (input, output) ->
                it("reverses $input to $output") { assert(input.reversed() == output) }
            }
        }
        describe("nested") {
            (1..10).forEach { i ->
                (1..10).forEach { j -> it("multiplies $i and $j to ${i*j}") {} }
            }
        }
    }
}
