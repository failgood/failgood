package failfast

@FailFastDSL
interface TestDSL {
    suspend fun println(body: String)
}

