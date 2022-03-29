package failgood.internal

import failgood.Success
import failgood.Test
import failgood.TestDSL
import failgood.TestResult
import failgood.describe
import failgood.mock.call
import failgood.mock.getCalls
import failgood.mock.mock
import kotlinx.coroutines.coroutineScope
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Test
class ResourcesCloserTest {
    val context = describe(ResourcesCloser::class) {
        val subject = coroutineScope { ResourcesCloser(this) }
        describe(ResourcesCloser::closeAutoCloseables.name) {
            it("closes autoclosables") {
                val autoCloseable = mock<AutoCloseable>()
                subject.autoClose(autoCloseable)
                subject.closeAutoCloseables()
                expectThat(getCalls(autoCloseable)).containsExactly(call(AutoCloseable::close))
            }
        }
        describe(ResourcesCloser::callAfterEach.name) {
            val testDSL = mock<TestDSL>()
            it("calls afterEach") {
                var called: Pair<TestDSL, TestResult>? = null
                subject.afterEach { success ->
                    called = Pair(this, success)
                }
                subject.callAfterEach(testDSL, Success(10))
                assert(called == Pair(testDSL, Success(10)))
            }
            pending("calls all after each methods even if one fails")
            pending("throws when one after each method failed")
        }
    }
}
