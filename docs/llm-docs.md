```kotlin
@Test
class MyFirstFailgoodTests {
    val tests = testCollection("my perfect test suite") {
        it("runs super fast") {
            assert(1 == 1)
        }
        describe("tests can be organized in sub-contexts") {
            it("just works") {
                assert(1 == 1)
            }
        }
    }
}
```
