@file:Suppress("KotlinConstantConditions")

package failgood.softly

import failgood.Test
import failgood.testCollection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.delay
import org.opentest4j.MultipleFailuresError

@Test
object SoftAssertTest {
    val tests =
        testCollection("soft asserts") {
            it("does not throw when all asserts are successful") {
                val name = "klausi"
                softly {
                    // standard boolean
                    assert(name == "klausi")
                    assert(name == "klausi") { "assert error message" }
                }
            }
            it("throws when one assert fails") {
                val name = "klausi"
                val exception =
                    assertNotNull(
                        runCatching {
                                softly {
                                    // standard boolean
                                    assert(name == "klausi")
                                    assert(name != "klausi") { "assert error message" }
                                }
                            }
                            .exceptionOrNull())
                assertEquals("assert error message", exception.message)
            }
            it("throws MultipleFailuresError when multiple asserts fail") {
                val name = "klausi"
                val exception =
                    assertNotNull(
                        runCatching {
                                softly {
                                    // standard boolean
                                    assert(name == "klausi")
                                    assert(name != "klausi") { "assert1 error message" }
                                    assert(name != "klausi") { "assert2 error message" }
                                }
                            }
                            .exceptionOrNull())
                assert(
                    exception is MultipleFailuresError &&
                        exception.failures.map { it.message }.toSet() ==
                            setOf("assert1 error message", "assert2 error message"))
            }
            it("can contain suspend methods") { softly { delay(1) } }
        }
}
