package failgood

@FailFastDSL
interface TestDSL : ResourcesDSL {
    suspend fun println(body: String)
}

