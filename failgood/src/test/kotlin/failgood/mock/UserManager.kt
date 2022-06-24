package failgood.mock

data class User(val name: String)

interface UserManager {
    fun overloadedFunction(): String
    fun overloadedFunction(s: String): String
    fun overloadedFunction(i: Int): String

    fun function()
    fun function2()
    fun functionWithParameters(number: Int, name: String): String
    fun functionWithDataClassParameters(user: User): String
    suspend fun suspendFunction(number: Int, name: String): String
    fun stringReturningFunction(): String
    fun functionThatReturnsNullableString(): String?
}
