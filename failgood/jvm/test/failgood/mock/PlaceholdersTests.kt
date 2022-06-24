package failgood.mock

import failgood.Test
import failgood.describe

@Test
class PlaceholdersTests {
    interface I {
        // see https://kotlinlang.org/docs/basic-types.html
        fun methodWithAllTypes(
            a: Byte,
            b: Short,
            c: Int,
            d: Long,
            e: Float,
            f: Double,
            g: Boolean,
            h: Char,
            i: String,
            j: Array<Byte>
        )
    }

    val tests = describe("parameter placeholders") {
        test("exist for all basic kotlin types") {
            mock<I> {
                method {
                    methodWithAllTypes(
                        anyByte(),
                        anyShort(),
                        anyInt(),
                        anyLong(),
                        anyFloat(),
                        anyDouble(),
                        anyBoolean(),
                        anyChar(),
                        any(),
                        any()
                    )
                }
            }
        }
    }
}
