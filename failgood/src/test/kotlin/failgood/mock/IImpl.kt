package failgood.mock

interface IImpl {
    fun overloadedFunction()
    fun overloadedFunction(s: String)
    fun overloadedFunction(i: Int)

    fun function()
    fun function2()
    fun functionWithParameters(number: Int, name: String)
    suspend fun suspendFunction(number: Int, name: String): String
    fun stringReturningFunction(): String
    fun functionThatReturnsNullableString(): String?
}
