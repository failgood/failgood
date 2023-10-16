package failgood

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

fun interface ContextProvider {
    fun getContexts(): List<RootContext>
}

class ObjectContextProvider(private val jClass: Class<out Any>) : ContextProvider {
    constructor(kClass: KClass<*>) : this(kClass.java)

    /** get root contexts from a class or object or defined at the top level */
    override fun getContexts(): List<RootContext> {
        // the RootContext constructor tries to determine its file and line number.
        // if the root context is created by a utility method outside the test class the file and
        // line info
        // points to the utility function instead of the test, and that's not what we want, so we
        // check if the
        // contexts we just loaded think that they are from the file we just loaded, and if they
        // don't we
        // overwrite that information with a pointer to the first line of the class we just loaded.

        return getContextsInternal().map {
            if (it.context.sourceInfo?.className != jClass.name)
                it.copy(context = it.context.copy(sourceInfo = SourceInfo(jClass.name, null, 1)))
            else it
        }
    }

    private fun getContextsInternal(): List<RootContext> {
        val obj =
            try {
                instantiateClassOrObject(jClass)
            } catch (e: InvocationTargetException) {
                throw ErrorLoadingContextsFromClass(
                    "Could not load contexts from class",
                    jClass.kotlin,
                    e.targetException
                )
            } catch (e: IllegalArgumentException) {
                // should we just ignore classes that fit the pattern but have no suitable
                // constructor?
                throw ErrorLoadingContextsFromClass(
                    "No suitable constructor found for class ${jClass.name}",
                    jClass.kotlin,
                    e
                )
            } catch (e: IllegalAccessException) { // just ignore private classes
                return listOf()
            }

        // get contexts from all methods returning RootContext or List<RootContext>
        val methodsReturningRootContext =
            jClass.methods
                .filter {
                    it.returnType == RootContext::class.java ||
                        it.returnType == List::class.java &&
                            it.genericReturnType.let { genericReturnType ->
                                genericReturnType is ParameterizedType &&
                                    genericReturnType.actualTypeArguments.singleOrNull() ==
                                        RootContext::class.java
                            }
                }
                .ifEmpty {
                    throw ErrorLoadingContextsFromClass("no contexts found in class", jClass.kotlin)
                }
        return methodsReturningRootContext.flatMap {
            val contexts = it.invoke(obj)
            @Suppress("UNCHECKED_CAST")
            contexts as? List<RootContext> ?: listOf(contexts as RootContext)
        }
    }

    companion object {
        fun instantiateClassOrObject(clazz: Class<out Any>): Any? {
            val instanceField =
                try {
                    clazz.getDeclaredField("INSTANCE")
                } catch (e: Exception) {
                    null
                }
            val obj =
                if (instanceField != null)
                // its a kotlin object
                instanceField.get(null)
                else {
                    // it's a kotlin class or a top level context
                    clazz.constructors.singleOrNull()?.newInstance()
                }
            return obj
        }
    }
}
