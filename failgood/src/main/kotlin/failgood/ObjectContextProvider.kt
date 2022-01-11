package failgood

import kotlin.reflect.KClass

fun interface ContextProvider {
    fun getContexts(): List<RootContext>
}

class ObjectContextProvider(private val jClass: Class<out Any>) : ContextProvider {
    constructor(kClass: KClass<*>) : this(kClass.java)

    /**
     * get root contexts from a class or object or defined at the top level
     */
    override fun getContexts(): List<RootContext> {
        val contexts: List<RootContext> = try {
            val instanceField = try {
                jClass.getDeclaredField("INSTANCE")
            } catch (e: Exception) {
                null
            }
            val obj = if (instanceField != null)
            // its a kotlin object
                instanceField.get(null)
            else
            // it's a kotlin class or a top level context
                jClass.constructors.singleOrNull()?.newInstance()

            // get contexts from all methods returning RootContext
            val methodsReturningRootContext = jClass.methods.filter { it.returnType == RootContext::class.java }
            // if there are no methods returning RootContext, maybe getContext returns a list of RootContexts
            val contextGetters = methodsReturningRootContext.ifEmpty { listOf(jClass.getDeclaredMethod("getContext")) }
            contextGetters.flatMap {
                val contexts = it.invoke(obj)
                @Suppress("UNCHECKED_CAST")
                contexts as? List<RootContext> ?: listOf(contexts as RootContext)
            }
        } catch (e: Exception) {
            listOf()
        }
        // now correct the sourceinfo if the context thinks it does not come from the class we just loaded
        return contexts.map {
            if (it.sourceInfo.className != jClass.name)
                it.copy(sourceInfo = SourceInfo(jClass.name, null, 1))
            else
                it
        }

        // slow failsafe version that uses kotlin reflect:
        //        return kClass.declaredMemberProperties.single { it.name == "context"
        // }.call(kClass.objectInstance) as RootContext
    }
}
