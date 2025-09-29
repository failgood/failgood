package com.example

import failgood.Test
import failgood.describe
import failgood.softly.softly
import kotlin.assert

@Test
class FailingTests {
    val tests =
        describe("failing tests") {
            test("failing test with kotlin asserts") { assert(1 == 2) }
            test("failing test with failgood soft asserts") { softly { assert(1 == 3) } }
        }
}
