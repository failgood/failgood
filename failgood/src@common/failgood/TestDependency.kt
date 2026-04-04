package failgood

import kotlin.reflect.KProperty

interface TestDependency<T> {
    operator fun getValue(owner: Any?, property: KProperty<*>): T
}
