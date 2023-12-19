package failgood.experiments.assertsuccess

import failgood.Context
import failgood.ExecutionListener
import failgood.SourceInfo
import failgood.Test
import failgood.describe
import failgood.experiments.assertsuccess.CheckResult.Success
import failgood.mock.getCalls
import failgood.mock.mock

private fun <E> List<E>.containsExactly(vararg matcher: (E) -> Boolean): CheckResult<Unit> {
    val errors =
        this.mapIndexed { idx: Int, e: E ->
                if (matcher[idx](e)) null else "element $e did not match"
            }
            .filterNotNull()
    return if (errors.isEmpty()) Success(Unit) else CheckResult.Failure(errors.joinToString())
}

@Test
object AssertSuccessTest {
    val context =
        describe("assertSuccess") {
            describe("is useful for asserting on mocks") {
                val listener = mock<ExecutionListener>()
                listener.contextDiscovered(
                    Context("name", sourceInfo = SourceInfo("blah", null, 1))
                )
                it("can return parameters for further asserting") {
                    with(
                        assert(
                            getCalls(listener)
                                .first()
                                .isCallTo(ExecutionListener::contextDiscovered)
                        )
                    ) {
                        assert(name == "name")
                    }
                }
            }
            describe("asserting on lists") {
                it("can use a function matcher") {
                    data class Song(val artist: String, val title: String)

                    val songs =
                        listOf(
                            Song("the cure", "close to me"),
                            Song("bruce springsteen", "born in the USA")
                        )

                    assert(
                        songs.containsExactly(
                            { it.artist == "the cure" },
                            { it.title == "born in the USA" }
                        )
                    )
                }
            }
        }
}
