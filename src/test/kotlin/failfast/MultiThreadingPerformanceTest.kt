package failfast

fun main() {
    Suite(listOf(MultiThreadingPerformanceTest.context), 1000).run().check()
}

object MultiThreadingPerformanceTest {
    val context = context {
        repeat(1000) {
            test("sleeping test $it") {
                @Suppress("BlockingMethodInNonBlockingContext")
                Thread.sleep(1000)
                print("T")
            }
        }
    }
}
