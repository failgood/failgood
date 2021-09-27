package failgood.internal

import failgood.junit.RootContextAndClass

internal interface TestFilterProvider {
    fun forContext(className: String, rootName: String): TestFilter
}

internal object ExecuteAllTestFilterProvider : TestFilterProvider {
    override fun forContext(className: String, rootName: String) = ExecuteAllTests
}

internal class RootContextAndClassTestFilterProvider(private val filterConfig: Map<RootContextAndClass, List<String>>) :
    TestFilterProvider {
    override fun forContext(className: String, rootName: String): TestFilter {
        return filterConfig[RootContextAndClass(className, rootName)]?.let { StringListTestFilter(it) }
            ?: ExecuteAllTests
    }
}
