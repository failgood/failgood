package failgood.internal

import failgood.junit.RootContextAndClass

internal interface TestFilterProvider {
    fun forContext(className: String, rootName: String): List<String>?
}

internal object ExecuteAllTestFilterProvider : TestFilterProvider {
    override fun forContext(className: String, rootName: String): List<String> {
        return listOf()
    }
}

internal class RootContextAndClassTestFilterProvider(private val filterConfig: Map<RootContextAndClass, List<String>>) :
    TestFilterProvider {
    override fun forContext(className: String, rootName: String): List<String>? {
        return filterConfig[RootContextAndClass(className, rootName)]
    }
}
