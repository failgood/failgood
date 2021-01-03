package failfast

fun main() {
    Suite(MultiThreadingPerformanceTestNotAlways.context, 1000).run().check()
}

object MultiThreadingPerformanceTestNotAlways {
    val context =
        context {
            repeat(1000) {
                test("sleeping test $it") {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    Thread.sleep(1000)
                }
            }
        }
}
