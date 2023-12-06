package failgood.experiments.stresstest

import failgood.RootContext
import failgood.dsl.ContextDSL

/*
 * A test that tries to stress the engine by running a lot of empty tests.
 * This did not reproduce the problem that I hoped it would reproduce,
 * but maybe the repeating of top level tests could become a feature,
 * so I'm keeping it for now.
 */

// @Test
class StressTest {
    val tests =
        runMultipleTimes("stress test", 100) {
            describe("query language") {
                describe("has a typesafe query api") {
                    it("blah") {}
                    it("blah 1") {}
                    it("blah 2") {}
                    test("blah 3") {}
                    describe("hmm") {
                        it("blah") {}
                        it("blah 1") {}
                        it("blah 2") {}
                        test("blah 3") {}
                        describe("hmmmmm") {
                            it("blah") {}
                            it("blah 1") {}
                            it("blah 2") {}
                            test("blah 3") {}
                        }
                    }
                }
            }
        }
}

fun runMultipleTimes(
    name: String,
    times: Int,
    tests: suspend ContextDSL<*>.() -> Unit
): List<RootContext> =
    (1..times).map { index -> RootContext(name + index, order = index) { tests() } }
