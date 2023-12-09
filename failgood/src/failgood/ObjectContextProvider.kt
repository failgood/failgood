package failgood

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

interface ContextCreator {
    val dependencies: List<Class<*>>
    val method: Method?

    fun getContexts(): List<RootContextWithGiven<*>>
}

fun interface ContextProvider {
    fun getContextCreators(): List<ContextCreator>
}

class ObjectContextProvider<Cls : Any>(private val jClass: Class<out Cls>) : ContextProvider {
    constructor(kClass: KClass<Cls>) : this(kClass.java)

    data class ContextCreatorImpl<Cls>(
        private val jClass: Class<out Cls>,
        private val instance: Any?,
        override val method: Method,
        override val dependencies: List<Class<*>>
    ) : ContextCreator {
        override fun getContexts(): List<RootContext> {
            val contexts =
                try {
                    // the most common case is that the getter has no parameters
                    if (method.parameters.isEmpty()) method.invoke(instance)
                    else {
                        // for private properties the kotlin compiler seems to sometimes generate a
                        // static getter that takes the instance as single parameter
                        val typeOfSingleParameter = method.parameters.singleOrNull()?.type
                        if (
                            typeOfSingleParameter != null &&
                                instance != null &&
                                typeOfSingleParameter == instance::class.java
                        )
                            method.invoke(null, instance)
                        else
                            throw ErrorLoadingContextsFromClass(
                                "context method ${method.niceString(jClass)} takes unexpected parameters",
                                jClass.kotlin
                            )
                    }
                } catch (e: ErrorLoadingContextsFromClass) {
                    throw e
                } catch (e: Exception) {
                    throw ErrorLoadingContextsFromClass(
                        "error invoking ${method.niceString(jClass)}",
                        jClass.kotlin,
                        e
                    )
                }
            @Suppress("UNCHECKED_CAST")
            return contexts as? List<RootContext> ?: listOf(contexts as RootContext)
        }
    }

    /** get root contexts from a class or object or defined at the top level */
    @Deprecated("use getContextCreators")
    fun getContexts(): List<RootContext> {
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
        val instance = createInstance()

        // get contexts from all methods returning RootContext or List<RootContext>
        val methodsReturningRootContext = getMethodsReturningContexts()
        return methodsReturningRootContext.flatMap { getContexts(it, instance) }
    }

    private fun getContexts(it: Method, instance: Any?): List<RootContext> {
        val contexts =
            try {
                // the most common case is that the getter has no parameters
                if (it.parameters.isEmpty()) it.invoke(instance)
                else {
                    // for private properties the kotlin compiler seems to sometimes generate a
                    // static getter that takes the instance as single parameter
                    val typeOfSingleParameter = it.parameters.singleOrNull()?.type
                    if (
                        typeOfSingleParameter != null &&
                            instance != null &&
                            typeOfSingleParameter == instance::class.java
                    )
                        it.invoke(null, instance)
                    else
                        throw ErrorLoadingContextsFromClass(
                            "context method ${it.niceString(jClass)} takes unexpected parameters",
                            jClass.kotlin
                        )
                }
            } catch (e: ErrorLoadingContextsFromClass) {
                throw e
            } catch (e: Exception) {
                throw ErrorLoadingContextsFromClass(
                    "error invoking ${it.niceString(jClass)}",
                    jClass.kotlin,
                    e
                )
            }
        @Suppress("UNCHECKED_CAST")
        return contexts as? List<RootContext> ?: listOf(contexts as RootContext)
    }

    private fun getMethodsReturningContexts(): List<Method> {
        val methodsReturningRootContext =
            jClass.methods
                .filter {
                    it.returnType == RootContextWithGiven::class.java ||
                        it.returnType == List::class.java &&
                            it.genericReturnType.let { genericReturnType ->
                                genericReturnType is ParameterizedType &&
                                    genericReturnType.actualTypeArguments.singleOrNull().let {
                                        actualTypArg ->
                                        actualTypArg is ParameterizedType &&
                                            actualTypArg.rawType == RootContextWithGiven::class.java
                                    }
                            }
                }
                .ifEmpty {
                    throw ErrorLoadingContextsFromClass("no contexts found in class", jClass.kotlin)
                }
        return methodsReturningRootContext
    }

    private fun createInstance(): Any? {
        val instance =
            try {
                instantiateClassOrObject(jClass)
            } catch (e: InvocationTargetException) {
                throw ErrorLoadingContextsFromClass(
                    "Could not load contexts from class",
                    jClass.kotlin,
                    e.targetException
                )
            } catch (e: IllegalArgumentException) {
                throw ErrorLoadingContextsFromClass(
                    "No suitable constructor found for class ${jClass.name}",
                    jClass.kotlin,
                    e
                )
            } catch (e: IllegalAccessException) { // just ignore private classes
                throw ErrorLoadingContextsFromClass(
                    "Test class ${jClass.name} is private. Just remove the @Test annotation if you don't want to run it, or make it public if you do.",
                    jClass.kotlin
                )
            }
        return instance
    }

    override fun getContextCreators(): List<ContextCreator> {
        val instance = createInstance()

        // get contexts from all methods returning RootContext or List<RootContext>
        val methodsReturningRootContext = getMethodsReturningContexts()
        return methodsReturningRootContext.map { method ->
            ContextCreatorImpl(
                jClass,
                instance,
                method,
                method.parameters
                    .map { parameter -> parameter.type }
                    .filter { clazz -> clazz != jClass }
            )
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

private fun Method.niceString(clazz: Class<*>) =
    "${clazz.name}.$name(${parameters.joinToString { it.name + " " + it.type.simpleName }})"
