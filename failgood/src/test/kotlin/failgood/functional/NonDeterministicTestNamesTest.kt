package failgood.functional

import failgood.Failed
import failgood.Suite
import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isNotNull
import strikt.assertions.single
import strikt.assertions.startsWith
import java.util.UUID

@Test
class NonDeterministicTestNamesTest {
    val context = describe("Non deterministic test names") {
        it("make their test count as failed") {
            expectThat(
                Suite {
                    it("test1" + UUID.randomUUID().toString()) {}
                    it("test2" + UUID.randomUUID().toString()) {}
                }.run(parallelism = 1, silent = true).failedTests
            ).single().and {
                get { test.testName }.startsWith("test2")
                get { result }.isA<Failed>().get { failure.message }.isNotNull()
                    .contains("please make sure your test names contain no random parts")
            }
        }
    }
}
