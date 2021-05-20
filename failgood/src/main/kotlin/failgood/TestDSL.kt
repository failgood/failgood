package failgood

@FailGoodDSL
interface TestDSL : ResourcesDSL {
    suspend fun println(body: String)
}

