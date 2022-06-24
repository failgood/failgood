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
import kotlin.test.assertNotNull

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
            describe("error handling") {
                val events = mutableListOf<String>()
                subject.afterEach {
                    events.add("afterEach1")
                    throw AssertionError("blah")
                }
                subject.afterEach {
                    events.add("afterEach2")
                    throw AssertionError("blah2")
                }
                it("calls all after each methods even if one fails") {
                    try {
                        subject.callAfterEach(testDSL, Success(10))
                    } catch (_: AssertionError) {
                    }
                    assert(events.containsAll(listOf("afterEach1", "afterEach2")))
                }
                it("throws the first exception  when multiple afterEach callbacks fail") {
                    val error = kotlin.runCatching {
                        subject.callAfterEach(testDSL, Success(10))
                    }
                    val exception = assertNotNull(error.exceptionOrNull())
                    assert(exception is AssertionError && exception.message == "blah")
                }
            }
        }
    }
}
