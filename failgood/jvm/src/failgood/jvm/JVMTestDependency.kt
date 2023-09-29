package failgood.jvm

import failgood.TestDependency
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class JVMTestDependency<T>(private val dependency: Deferred<Result<T>>) : TestDependency<T> {
    override operator fun getValue(owner: Any?, property: KProperty<*>): T {
        return runBlocking { dependency.await().getOrThrow() }
    }
}
