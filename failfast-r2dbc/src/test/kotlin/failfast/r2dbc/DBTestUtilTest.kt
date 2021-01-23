package failfast.r2dbc

import failfast.FailFast
import failfast.describe
import kotlinx.coroutines.reactive.awaitSingle

fun main() {
    FailFast.runTest()
}
val dbs = DBTestUtil("failfast-r2dbc")

object DBTestUtilTest {
    val context = describe("r2dbc test tools") {
        context("can run tests on h2 and psql") {
            forAllDatabases(dbs) { connectionFactory ->
                it("works") {
                    connectionFactory.create().awaitSingle()
                }
            }
        }
    }
}
