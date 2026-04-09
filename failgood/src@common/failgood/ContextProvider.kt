package failgood

fun interface ContextProvider {
    fun getContexts(): List<TestCollection<*>>
}
