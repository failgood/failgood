package failgood.internal

import failgood.Test
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
        it("closes autoclosables") {
            val autoCloseable = mock<AutoCloseable>()
            subject.autoClose(autoCloseable)
            subject.close()
            expectThat(getCalls(autoCloseable)).containsExactly(call(AutoCloseable::close))
        }
        it("calles afterEach") {
            var called = false
            subject.afterEach {
                called = true
            }
            subject.close()
            assert(called)
        }
    }
}
