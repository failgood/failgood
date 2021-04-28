package failfast

@FailFastDSL
interface TestDSL : ResourcesDSL {
    suspend fun println(body: String)
}

