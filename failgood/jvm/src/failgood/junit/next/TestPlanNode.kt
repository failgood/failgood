package failgood.junit.next

sealed interface TestPlanNode {
    val name: String

    data class Test(override val name: String) : TestPlanNode

    data class Container(override val name: String, val children: Set<TestPlanNode> = setOf()) :
        TestPlanNode
}
