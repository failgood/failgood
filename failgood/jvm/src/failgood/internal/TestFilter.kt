package failgood.internal

internal interface TestFilter {
    /** return true when this context path needs to be executed to satisfy the filter. */
    fun shouldRun(testPath: Path): Boolean
}

internal class StringListTestFilter(internal val filterList: List<String>) : TestFilter {
    override fun shouldRun(testPath: Path): Boolean {
        val path = testPath.path
        val smallerSize = minOf(filterList.size, path.size)
        return filterList.subList(0, smallerSize) == path.subList(0, smallerSize)
    }
}

internal object ExecuteAllTests : TestFilter {
    override fun shouldRun(testPath: Path) = true
}
