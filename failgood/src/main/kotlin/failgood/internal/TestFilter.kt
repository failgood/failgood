package failgood.internal

internal interface TestFilter {
    /**
     * return true when this context path needs to be executed to satisfy the filter.
     */
    fun shouldRun(testPath: ContextPath): Boolean
}

internal class StringListTestFilter(internal val filterList: List<String>) : TestFilter {
    override fun shouldRun(testPath: ContextPath): Boolean {
        val path = testPath.container.path + testPath.name
        val smallerSize = minOf(filterList.size, path.size)
        return filterList.subList(0, smallerSize) == path.subList(0, smallerSize)
    }
}
internal object ExecuteAllTests : TestFilter {
    override fun shouldRun(testPath: ContextPath) = true
}
