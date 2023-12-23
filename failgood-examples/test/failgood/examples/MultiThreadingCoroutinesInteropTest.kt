package failgood.examples

import failgood.FailGood
import failgood.Test
import failgood.testsAbout
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import strikt.api.expectThat
import strikt.assertions.isEqualTo

fun main() {
    FailGood.runTest()
}

class StringProvider {
    @Suppress("RedundantSuspendModifier", "SameReturnValue") suspend fun world() = "world"
}

class Example(private val stringProvider: StringProvider) {

    suspend fun hello(): String {
        delay(250)
        return stringProvider.world()
    }
}

@Test
class MultiThreadingCoroutinesInteropTest {
    val context =
        testsAbout("multi threading issue from spek") {
            val stringProvider = mockk<StringProvider> { coEvery { world() } returns "world 2" }
            val example = Example(stringProvider)

            describe("ExampleTest") {
                it("should work ;)") {
                    expectThat(example.hello()).isEqualTo("world 2")

                    coVerify { stringProvider.world() }
                }
            }
        }
}
