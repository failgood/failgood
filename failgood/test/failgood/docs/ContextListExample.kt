package failgood.docs

import failgood.Test
import failgood.testCollection

@Test
class ContextListExample {
    /*
     contexts can be declared as a list. this can make sense in combination with context order to speed up your suite.
     for example in an orm test suite you could have a utility method that creates tests for different databases,
     and have that method create the tests that use H2 with order 0 and tests that need a postgresql container with order 1, while
     also starting the postgresql container in a separate thread in a static initializer.
     that way the tests that don't need the container will run first while the postgres container starts in the background
     and is ready for the other tests later.
    */
    val contexts =
        listOf(
            testCollection("first of multiple contexts defined in one object", order = 0) {
                it("describes behavior") { assert("test" == "test") }
            },
            testCollection("second of multiple contexts defined in one object", order = 1) {
                it("describes behavior") { assert("test" == "test") }
            })
}
