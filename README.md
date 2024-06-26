[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.failgood/failgood/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.failgood/failgood)
[![Github CI](https://github.com/failgood/failgood/workflows/CI/badge.svg)](https://github.com/failgood/failgood/actions)
[![codecov](https://codecov.io/gh/failgood/failgood/branch/main/graph/badge.svg?token=Y8I6J84BHZ)](https://codecov.io/gh/failgood/failgood)

# FailGood

Failgood is a test runner for Kotlin focusing on simplicity, usability and speed.
[more...](docs/the%20philosophy%20of%20failgood.md)

```kotlin
@Test
class MyFirstFailgoodTest {
    val tests = testCollection {
        it("runs super fast") {
            assert(true)
        }
        describe("tests can be organized in subcontexts") {
            it("just works") {}
        }
    }
}
```
[Failgood in 5 minutes](docs/failgood%20in%205%20minutes.md)

## What's so cool about it?

### It's Fast
While other test-runners are still busy scanning your classpath for tests, failgood is already running them in parallel, and if your test suite is lightweight and well written maybe its already finished.
This is failgood running its own test suite on a Macbook Pro M1:

```
239 tests. 235 ok, 4 pending. time: 479ms. load:508%. 498 tests/sec
```
239 Tests in 0.4 seconds. Your test suite could be so fast too.

### Boring
Failgood is boring at runtime. Every test runs with fresh dependencies, just like in JUnit, see [test lifecycle](docs/how%20to%20write%20tests%20with%20failgood.md). For advanced lifecycle option look at [More on lifecycle](docs/isolation.md)

Everything is just kotlin.
Want to reuse a group of tests? extract a function for them.

Want to run a test conditionally? put an `if` around it

To create parameterized tests use `forEach` [more...](docs/parametrized%20tests.md)

Failgood works well with [IntelliJ IDEA](docs/idea%20support.md), [Gradle](docs/gradle.md), your favorite [assertion library](docs/assertion%20libraries.md) and [code coverage tools](docs/coverage.md).

### Contributing

Contributions are always welcome, please look [here](./docs/contributing.md)

### Example test suites

To see it in action check out the [failgood-example project](./failgood-examples), or a project that uses Failgood, for example
[the "the.orm" test suite](https://github.com/christophsturm/the.orm)
or [the restaurant test suite](https://github.com/christophsturm/restaurant/tree/main/core/src/test/kotlin/restaurant)



