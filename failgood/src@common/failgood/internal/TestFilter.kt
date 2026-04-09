package failgood.internal

interface TestFilter {
    fun shouldRun(testPath: Path): Boolean
}

internal class StringListTestFilter(private val filters: List<String>) : TestFilter {
    internal val filterList: List<String>
        get() = filters

    override fun shouldRun(testPath: Path): Boolean {
        val path = testPath.path
        val smallerSize = minOf(filters.size, path.size)
        val expected = filters.subList(0, smallerSize)
        val actual = path.subList(0, smallerSize)
        return expected == actual
    }
}

internal object ExecuteAllTests : TestFilter {
    override fun shouldRun(testPath: Path): Boolean = true
}

interface TestFilterProvider {
    fun forClass(className: String): TestFilter
}

internal object ExecuteAllTestFilterProvider : TestFilterProvider {
    override fun forClass(className: String): TestFilter = ExecuteAllTests
}

internal class StaticTestFilterProvider(private val filter: TestFilter) : TestFilterProvider {
    override fun forClass(className: String): TestFilter = filter
}

internal class ClassTestFilterProvider(private val filterConfig: Map<String, List<String>>) :
    TestFilterProvider {
    override fun forClass(className: String): TestFilter {
        return filterConfig[className]?.let(::StringListTestFilter) ?: ExecuteAllTests
    }
}
