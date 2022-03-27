package failgood

import failgood.internal.FailedRootContext

@Test
class SuiteResultTest {
    val context = describe(SuiteResult::class) {
        describe("when there are failing contexts") {
            val result = SuiteResult(
                listOf(),
                listOf(),
                listOf(),
                listOf(FailedRootContext(Context("context"), failure = RuntimeException()))
            )
            test("allOk is false") {
                assert(!result.allOk)
            }
        }
    }
}
