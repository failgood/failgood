package failgood

import failgood.junit.RootContextInClass

class TestFilterProvider(private val filterConfig: Map<RootContextInClass, List<String>>) {
    fun forContext(className: String, rootName: String): List<String>? {
        return filterConfig.get(RootContextInClass(className, rootName))
    }

}
