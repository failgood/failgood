package failgood

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class TestDependency<T>(private val dependency: Deferred<T>) {
    operator fun getValue(owner: Any?, property: KProperty<*>): T {
        return runBlocking { dependency.await() }
    }
}
