package failfast

import kotlin.reflect.KProperty

class TestDependency<T>(val dependency: T) {
    operator fun getValue(owner: Any?, property: KProperty<*>): T {
        return dependency
    }
}
