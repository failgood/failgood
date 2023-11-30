package failgood.internal

interface TestFilter {
    /** return true when this context path needs to be executed to satisfy the filter. */
    fun shouldRun(testPath: Path): Boolean
}

internal class StringListTestFilter(internal val filterList: List<String>) : TestFilter {
    override fun shouldRun(testPath: Path): Boolean {
        val path = testPath.path
        val smallerSize = minOf(filterList.size, path.size)
        val expected = filterList.subList(0, smallerSize)
        val actual = path.subList(0, smallerSize)
        return expected == actual
    }
}

internal object ExecuteAllTests : TestFilter {
    override fun shouldRun(testPath: Path) = true
}

interface TestFilterProvider {
    fun forClass(className: String): TestFilter
}

internal object ExecuteAllTestFilterProvider : TestFilterProvider {
    override fun forClass(className: String) = ExecuteAllTests
}

/** Return always the same filter */
internal class StaticTestFilterProvider(val filter: TestFilter) : TestFilterProvider {
    override fun forClass(className: String) = filter
}

internal class ClassTestFilterProvider(private val filterConfig: Map<String, List<String>>) :
    TestFilterProvider {
    override fun forClass(className: String): TestFilter {
        return filterConfig[className]?.let { StringListTestFilter(it) } ?: ExecuteAllTests
    }
}
