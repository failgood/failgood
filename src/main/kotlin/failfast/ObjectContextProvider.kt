package failfast

fun interface ContextProvider {
    fun getContext(): RootContext
}

class ObjectContextProvider(private val jClass: Class<out Any>) : ContextProvider {
    override fun getContext(): RootContext {
        val obj = jClass.getDeclaredField("INSTANCE").get(null)
        return jClass.getDeclaredMethod("getContext").invoke(obj) as RootContext
// slow failsafe version that uses kotlin reflect:
        //        return kClass.declaredMemberProperties.single { it.name == "context" }.call(kClass.objectInstance) as RootContext
    }
}

