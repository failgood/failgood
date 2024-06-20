package failgood

fun main() {
    Suite(MultiThreadingPerformanceTest().tests).run(1000, silent = true).check()
}

class MultiThreadingPerformanceTest {
    val tests =
        testCollection("multi threaded test running") {
            repeat(1000) {
                test("sleeping test ${it + 1}") {
                    Thread.sleep(1000)
                }
            }
        }
}
