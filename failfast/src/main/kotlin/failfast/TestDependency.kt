package failfast

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class TestDependency<T>(val dependency: Deferred<T>) {
    operator fun getValue(owner: Any?, property: KProperty<*>): T {
        return runBlocking { dependency.await() }
    }
}
