[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.failgood/failgood/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.failgood/failgood)
[![Github CI](https://github.com/failgood/failgood/workflows/CI/badge.svg)](https://github.com/failgood/failgood/actions)

# FailGood

Multi-threaded test runner for Kotlin focusing on simplicity, usability and speed. Now including a simple mock library.
Still zero dependencies.

## Goals / Features

Every design decision is only influenced by what's best for a short test feedback loop, and to make simple things simple
and complex things possible. No feature exists "because that's how JUnit works". Everything is driven by the needs of
people who write tests daily and iterate fast.

* Spec syntax implemented to work just [as expected](https://en.wikipedia.org/wiki/Principle_of_least_astonishment).
* Speed and parallel execution. FailGood's own test suite runs in < 1 second.
* Configuration via API. Your test config is just a main method that runs via IDEA or Gradle.
* Run your tests so fast that you can run all the tests on every change.
* Autotest to run only changed tests.
* Pitest plugin (see the build file).
* JUnit compatible reports

## How it looks like

```kotlin
@Testable
class FailGoodTest {
    val context = describe("The test runner") {
        it("supports describe/it syntax") { expectThat(true).isEqualTo(true) }
        describe("nested contexts") {
            it("can contain tests too") { expectThat(true).isEqualTo(true) }

            describe("disabled/pending tests") {
                pending("pending can be used to mark pending tests") {}
                pending("for pending tests the test body is optional")
            }
            context("context/test syntax is also supported") {
                test(
                    "I prefer describe/it but if there is no subject to describe I use " +
                            "context/test"
                ) {}
            }

            context("dynamic tests") {
                (1 until 5).forEach { contextNr ->
                    context("dynamic context #$contextNr") {
                        (1 until 5).forEach { testNr ->
              test("test #$testNr") {
                expectThat(testNr).isLessThan(10)
                expectThat(contextNr).isLessThan(10)
              }
            }
          }
        }
      }
    }
  }
}

```

To see it in action check out the failgood-example project, or a project that uses FailGood, for example
[the r2dbcfun test suite](https://github.com/christophsturm/r2dbcfun/blob/main/src/test/kotlin/r2dbcfun/test/AllTests.kt)

## Running the test suite

to run FailGood's test suite just run `./gradlew check` or if you want to run it via idea just run
the `FailGoodBootstrap.kt` class.

## Gradle build

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("com.christophsturm.failgood:failgood:0.3.0")
}
```

## Running

There are two ways to run your failgood suite:

* A main method that calls `FailGood.runAllTests()`
* A junit-platform engine

### the junit-platform-engine

To use the JUnit engine add the JUnit platform launcher to your gradle file dependency block:

```kotlin
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.0")
```

Make sure to select "run tests in IDEA" in your gradle settings, running via gradle is currently not supported (coming
soon)

### The "main method method"


```kotlin
fun main() {
    // this will find tests in all files named *Test in the same source root as the main class
    FailGood.runAllTests() // or runAllTests(true) to write a junit test report
}

```

alternatively you can also just manually list test contexts:

```kotlin
fun main() {
    Suite.fromContexts(listOf(MyTest.context, MyOtherTest.context)).run().check()
}
```

or test classes (possibly slightly faster than listing contexts, because more work is done in parallel)

```kotlin
fun main() {
    Suite.fromClasses(listOf(MyTest::class, MyOtherTest::class)).run().check()
}
```

then add a gradle task file that calls it with the test classpath and make your check target depend on it.

```kotlin
val testMain = tasks.register("testMain", JavaExec::class) {
    main = "<my-package>.FailGoodMainKt"
    classpath = sourceSets["test"].runtimeClasspath
    }

tasks.check { dependsOn(testMain) }
```

`./gradlew check` will then run the tests.

## Running a single test file

You can also run a single file with the "main method method":

```kotlin
fun main() {
    FailGood.runTest() // will run tests in the current file
}

object MyTestClass {
    val context = describe("my favorite class") {
        // ...
    }
}

```

...and run that.

## Running a single test

Sometimes a test keeps failing, and you want to run only that single test until it is fixed, for easier debugging.

just take the test description from the test output:

```
failed tests:
Test Running > a failed test > can be run again: failed with strikt.internal.opentest4j.MappingFailed ....
```

...and add it to the run test line.

```kotlin
fun main() {
    FailGood.runTest("Test Running > a failed test > can be run again")
}
```

## Test lifecycle

Just declare your dependencies in the context blocks. They will be recreated for every test. It just works as expected.
I think ScalaTest has a mode that works like that and kotest also supports it, and calls
it  [instance per leaf](https://github.com/kotest/kotest/blob/master/doc/isolation_mode.md#instanceperleaf)

It combines the power of a dsl with the simplicity of JUnit 4.

this is from the test isolation unit test:

```kotlin
    val context =
    describe("test dependencies") {
        it("are recreated for each test") {

            // the total order of events is not defined because tests run in parallel.
            // so we track events in a list of a list and record the events that lead to each test execution
            val totalEvents = mutableListOf<List<String>>()
            Suite {
                val testEvents = mutableListOf<String>()
                totalEvents.add(testEvents)
                testEvents.add(ROOT_CONTEXT_EXECUTED)
                autoClose("dependency", closeFunction = { testEvents.add(DEPENDENCY_CLOSED) })
                test("test 1") { testEvents.add(TEST_1_EXECUTED) }
                test("test 2") { testEvents.add(TEST_2_EXECUTED) }
                context("context 1") {
                    testEvents.add(CONTEXT_1_EXECUTED)

                    context("context 2") {
                        testEvents.add(CONTEXT_2_EXECUTED)
                        test("test 3") { testEvents.add(TEST_3_EXECUTED) }
                    }
                }
                test("test4: tests can be defined after contexts") {
                    testEvents.add(TEST_4_EXECUTED)
                }
            }.run()

            expectThat(totalEvents)
                .containsExactlyInAnyOrder(
                    listOf(ROOT_CONTEXT_EXECUTED, TEST_1_EXECUTED, DEPENDENCY_CLOSED),
                    listOf(ROOT_CONTEXT_EXECUTED, TEST_2_EXECUTED, DEPENDENCY_CLOSED),
                    listOf(
                        ROOT_CONTEXT_EXECUTED,
                        CONTEXT_1_EXECUTED,
                        CONTEXT_2_EXECUTED,
                        TEST_3_EXECUTED,
                        DEPENDENCY_CLOSED
                    ),
                    listOf(ROOT_CONTEXT_EXECUTED, TEST_4_EXECUTED, DEPENDENCY_CLOSED)
                )
        }
    }
```

## failgood?

It's pretty fast. its own test suite runs in less than one second:

```kotlin
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
expectThat(uptime).isLessThan(1000) // lets see how far we can get with one second
```

## autotest

add a main method that just runs autotest:

```kotlin
fun main() {
    autoTest()
}
```

create a gradle exec task for it:

```kotlin
tasks.register("autotest", JavaExec::class) {
    main = "failgood.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}
```

run it with `./gradlew -t autotest`anytime a test file is recompiled it will run. This works pretty well, but it's not
perfect, because not every change to a tested class triggers a recompile of the test class. Fixing this by reading
dependencies from the test classes' constant pool is on the roadmap.

## Even faster tests; best practices

* avoid heavyweight dependencies. the failgood test suite runs in < 1000ms. That's a lot of time for a computer, and a
  great target for your test suite. Slow tests are a code smell. An unexpected example for a heavyweight dependency is
  mockk, it takes about 2 seconds at first invocation.


* spin up slow dependencies at start-up in a separate thread (if you have to use them). For example this is the main
  test method of another open source project of mine:

```kotlin
  fun main() {
    thread {
        JvmMockKGateway() // mockk takes 2 seconds at its first invocation
    }
    if (!H2_ONLY)
        thread {
            postgresqlcontainer // spin up the postgres container if needed
        }
    Suite.fromClasses(findTestClasses(TransactionFunctionalTest::class)).run().check()
}
```

## Test coverage

You can get a quick overview of your test coverage by running your test main method with the idea coverage runner.

There is also a pitest plugin if you want to measure mutation coverage, see the example project for configuration.

## Avoiding global state

* if you need a web server run it on a random port.
* if you need a database create a db with a random name for each test. (see the.orm)
  or run the test in a transaction that is rolled back at the end
