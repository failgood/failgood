package failgood

fun main() {
    Suite(MultiThreadingPerformanceTestX().context).run(1000, silent = true).check()
}

@Test
class MultiThreadingPerformanceTestX {
    val context =
        describe("multi threaded test running") {
            repeat(1000) {
                test("sleeping test ${it + 1}") {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(1000)
                }
            }
        }
}
