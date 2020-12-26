package failfast

import strikt.api.expectThat
import strikt.assertions.hasSize
import kotlin.system.measureTimeMillis

fun main() {
    Suite(TestFinderTest.context).run().check()
}

object TestFinderTest {
    val context = describe("test finder") {
        it("can find Test classes") {
            println(measureTimeMillis {
                expectThat(FailFast.findTestClasses(TestFinderTest::class)).hasSize(12)
            })
        }
    }
}



