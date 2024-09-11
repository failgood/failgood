### Parametrized tests

Failgood needs no special support for parametrized tests. You can just use `forEach` to create multiple versions of a test

```kotlin
val tests = testCollection("String#reverse") {
    listOf(Pair("otto", "otto"), Pair("racecar", "racecar")).forEach { (input, output) ->
        it("reverses $input to $output") {
            assert(input.reversed() == output)
        }
    }
}

```
In the case of the above example you may even want to add more test inputs and outputs for better coverage.
