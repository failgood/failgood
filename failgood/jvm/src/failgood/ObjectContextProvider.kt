package failgood

import failgood.internal.SourceInfo
import java.lang.reflect.InvocationTargetException
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
        // the RootContext constructor tries to determine its file and line number.
        // if the root context is created by a utility method outside the test class the file and line info
        // points to the utility function instead of the test, and that's not what we want, so we check if the
        // contexts we just loaded think that they are from the file we just loaded, and if they don't we
        // overwrite that information with a pointer to the first line of the class we just loaded.

        return getContextsInternal().map {
            if (it.sourceInfo.className != jClass.name)
                it.copy(sourceInfo = SourceInfo(jClass.name, null, 1))
            else
                it
        }
    }

    private fun getContextsInternal(): List<RootContext> {
        val instanceField = try {
            jClass.getDeclaredField("INSTANCE")
        } catch (e: Exception) {
            null
        }
        val obj = if (instanceField != null)
        // its a kotlin object
            instanceField.get(null)
        else {
            // it's a kotlin class or a top level context
            jClass.constructors.singleOrNull()?.let {
                try {
                    it.newInstance()
                } catch (e: InvocationTargetException) {
                    throw ErrorLoadingContextsFromClass("Could not load contexts from class", jClass, e.targetException)
                } catch (e: IllegalArgumentException) {
                    // should we just ignore classes that fit the pattern but have no suitable constructor?
                    throw ErrorLoadingContextsFromClass(
                        "No suitable constructor found for class ${jClass.name}", jClass, e
                    )
                } catch (e: IllegalAccessException) { // just ignore private classes
                    return listOf()
                }
            }
        }

        // get contexts from all methods returning RootContext
        val methodsReturningRootContext = jClass.methods.filter { it.returnType == RootContext::class.java }
        // if there are no methods returning RootContext, maybe getContext returns a list of RootContexts
        val contextGetters = methodsReturningRootContext.ifEmpty {
            try {
                listOf(jClass.getDeclaredMethod("getContext"))
            } catch (e: Exception) {
                throw ErrorLoadingContextsFromClass("no contexts found in class", jClass)
            }
        }
        return contextGetters.flatMap {
            val contexts = it.invoke(obj)
            @Suppress("UNCHECKED_CAST")
            contexts as? List<RootContext> ?: listOf(contexts as RootContext)
        }
    }
}
