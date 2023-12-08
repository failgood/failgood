package failgood.junit.next

sealed interface TestPlanNode {
    val name: String
    val displayName: String

    data class Test(override val name: String, override val displayName: String = name) :
        TestPlanNode {

        init {
            if (name.isBlank()) throw IllegalArgumentException("name must not be blank")
        }
    }

    data class Container(override val name: String, override val displayName: String = name) :
        TestPlanNode {
        init {
            if (name.isBlank()) throw IllegalArgumentException("name must not be blank")
        }
    }
}
