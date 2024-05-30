package failgood.jvm

import failgood.TestDependency
import kotlin.reflect.KProperty
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

class JVMTestDependency<T>(private val dependency: Deferred<Result<T>>) : TestDependency<T> {
    override operator fun getValue(owner: Any?, property: KProperty<*>): T {
        return runBlocking { dependency.await().getOrThrow() }
    }
}
