### Given

Contexts can define a `given` block, which will be evaluated for every test and can be accessed inside the tests via a `given` getter.

```kotlin
describe("context with given lambda", given = { "StringDependency" }) {
    test("test that takes a string dependency") {
        expectThat(given).isEqualTo("StringDependency")
    }
}
```

#### Nesting Given blocks

Only the given block of the context containing the test is always evaluated. parent context given blocks are evaluated lazily when accessed from the given block.

an example from `GivenTest.kt`:

```kotlin
                describe("the given of a parent context", given = { "parentContextGiven" }) {
                    describe(
                        "is available in the given block of the child",
                        given = {
                            val parentGiven = given()
                            assertEquals("parentContextGiven", parentGiven)
                            "ok"
                        }
                    ) {
                        it("first test") { assertEquals("ok", given) }
                    }
                }
```

This may sound cool, but it is really not a good idea to use highly nested given blocks. Keep your test context self-contained, nobody wants to go hunting in parent contexts to see all dependencies on a test. If you want to reuse things put them into a class or into functions.

#### Given and isolation

Given blocks are evaluated for each test even if context isolation is turned off. Turning off context isolation and putting all test dependencies in the given block
is a great way to write good readable fast tests.
