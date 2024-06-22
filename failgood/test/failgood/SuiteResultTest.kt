package failgood

import failgood.internal.FailedTestCollectionExecution

@Test
class SuiteResultTest {
    val tests =
        testCollection(SuiteResult::class) {
            describe("when there are failing contexts") {
                val result =
                    SuiteResult(
                        listOf(),
                        listOf(),
                        listOf(),
                        listOf(FailedTestCollectionExecution(Context("context"), failure = RuntimeException()))
                    )
                test("allOk is false") { assert(!result.allOk) }
            }
        }
}
