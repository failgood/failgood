package failgood

import failgood.internal.FailedContext

@Test
class SuiteResultTest {
    val context = describe(SuiteResult::class) {
        describe("when there are failing contexts") {
            val result = SuiteResult(
                listOf(), listOf(), listOf(), listOf(FailedContext(Context("context"), failure = RuntimeException()))
            )
            test("allOk is false") {
                assert(!result.allOk)
            }
        }
    }
}
