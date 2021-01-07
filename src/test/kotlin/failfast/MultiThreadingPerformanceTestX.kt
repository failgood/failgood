package failfast

fun main() {
    Suite(MultiThreadingPerformanceTestX.context).run(1000).check()
}

object MultiThreadingPerformanceTestX {
    val context =
        describe("multi threaded test running") {
            repeat(1000) {
                test("sleeping test $it") {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(1000)
                }
            }
        }
}
