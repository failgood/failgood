package failfast

fun interface ContextProvider {
    fun getContext(): RootContext
}

class ObjectContextProvider(private val jClass: Class<out Any>) : ContextProvider {
    override fun getContext(): RootContext {
        return try {
            val instanceField = jClass.getDeclaredField("INSTANCE")
            val obj = instanceField.get(null)
            jClass.getDeclaredMethod("getContext").invoke(obj) as RootContext
        } catch (e: Exception) {
            try {
                jClass.getDeclaredMethod("getContext").invoke(null) as RootContext
            } catch (e: Exception) {
                throw FailFastException(
                    "no idea how to find context in $jClass. declared fields:" +
                        jClass.declaredFields.joinToString { it.name }
                )
            }
        }
        // slow failsafe version that uses kotlin reflect:
        //        return kClass.declaredMemberProperties.single { it.name == "context"
        // }.call(kClass.objectInstance) as RootContext
    }
}
