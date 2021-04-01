package failfast

import kotlin.reflect.KClass

fun interface ContextProvider {
    fun getContexts(): List<RootContext>
}

class ObjectContextProvider(private val jClass: Class<out Any>) : ContextProvider {
    constructor(kClass: KClass<*>) : this(kClass.java)

    override fun getContexts(): List<RootContext> {
        return try {
            val obj = instantiateKotlinObject(jClass)
            val contexts = jClass.getDeclaredMethod("getContext").invoke(obj)
            @Suppress("UNCHECKED_CAST")
            contexts as? List<RootContext> ?: listOf(contexts as RootContext)
        } catch (e: Exception) {
            try {
                listOf(jClass.getDeclaredMethod("getContext").invoke(null) as RootContext)
            } catch (e: Exception) {
                return listOf()
            }
        }
        // slow failsafe version that uses kotlin reflect:
        //        return kClass.declaredMemberProperties.single { it.name == "context"
        // }.call(kClass.objectInstance) as RootContext
    }

    private fun instantiateKotlinObject(clazz: Class<out Any>): Any = clazz.getDeclaredField("INSTANCE").get(null)
}
