@file:Suppress("KotlinConstantConditions")

package failgood.softly

import failgood.Test
import failgood.testCollection
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
                val message = exception.message
                assert(message != null && message.startsWith("assert error message"))
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
                assert(exception is MultipleFailuresError)
                val failures = (exception as MultipleFailuresError).failures
                assert(failures.size == 2)
                val firstMessage = failures[0].message
                assert(firstMessage != null && firstMessage.startsWith("assert1 error message"))
                val secondMessage = failures[1].message
                assert(secondMessage != null && secondMessage.startsWith("assert2 error message"))
            }
            it("can contain suspend methods") { softly { delay(1) } }
        }
}
