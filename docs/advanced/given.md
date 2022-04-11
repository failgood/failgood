### Given

Sub-contexts can define a `given` block, whose result will be freshly evaluated and passed to each test. This is not different from dependencies that are just declared in the context, those are also freshly evaluated
for each test. The only real difference is that the given block is evaluated as part of the test, which can result in better parallelization of test runs.

```kotlin
context(
    "context with dependency lambda",
    given = { "StringDependency" }
) {
    test("test that takes a string dependency") { givenString ->
        expectThat(givenString).isEqualTo("StringDependency")
    }
}
```

Given support is an alternative way to declare your dependencies or to do things before each test. It's not something you have to use, it's perfectly fine to declare all dependencies directly in the context.
If you like it try it out and if you don't like it just ignore it and don't worry about it.
