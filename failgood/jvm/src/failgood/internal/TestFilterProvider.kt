package failgood.internal

interface TestFilterProvider {
    fun forClass(className: String): TestFilter
}

internal object ExecuteAllTestFilterProvider : TestFilterProvider {
    override fun forClass(className: String) = ExecuteAllTests
}

internal class StaticTestFilterProvider(val filter: TestFilter) : TestFilterProvider {
    override fun forClass(className: String) = filter
}

internal class ClassTestFilterProvider(private val filterConfig: Map<String, List<String>>) :
    TestFilterProvider {
    override fun forClass(className: String): TestFilter {
        return filterConfig[className]?.let { StringListTestFilter(it) } ?: ExecuteAllTests
    }
}
