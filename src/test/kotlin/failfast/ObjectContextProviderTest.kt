package failfast

import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

fun main() {
    Suite(ObjectContextProviderTest.context).run()
}

object ObjectContextProviderTest {
    val context = describe(ObjectContextProvider::class) {
        it("provides a context from an object") {
            print(measureTimeMillis {
                expectThat(ObjectContextProvider(TestFinderTest::class).getContext()).isA<RootContext>().and {
                    get(RootContext::name).isEqualTo("test finder")
                }
            })
        }

    }

}

class ObjectContextProvider(val kClass: KClass<*>) {
    fun getContext(): RootContext {
        val jClass = kClass.java
        val obj = jClass.getDeclaredField("INSTANCE").get(null)
        return jClass.getDeclaredMethod("getContext").invoke(obj) as RootContext
// slow failsafe version that uses kotlin reflect:
        //        return kClass.declaredMemberProperties.single { it.name == "context" }.call(kClass.objectInstance) as RootContext
    }
}
