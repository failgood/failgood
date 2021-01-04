[![Download](https://api.bintray.com/packages/christophsturm/maven/failfast/images/download.svg)](https://bintray.com/christophsturm/maven/failfast/_latestVersion)
[![Github CI](https://github.com/christophsturm/failfast/workflows/CI/badge.svg)](https://github.com/christophsturm/failfast/actions)

# Failfast

multi threaded test runner for kotlin focusing on simplicity and speed.

## goals

* spec syntax implemented to work just [as expected](https://en.wikipedia.org/wiki/Principle_of_least_astonishment)
* speed and parallel execution (own test suite runs in < 1 second)
* really simple. no dependencies and not a lot of code

## how it looks like

```kotlin
object FailFastTest {
    val context = describe("The test runner") {
        it("supports describe/it syntax") { expectThat(true).isEqualTo(true) }
        describe("nested contexts") {
            it("can contain tests too") { expectThat(true).isEqualTo(true) }

            describe("disabled/pending tests") {
                itWill("itWill can be used to mark pending tests") {}
                itWill("for pending tests the test body is optional")
                test("tests without body are pending")
            }
            context("context/test syntax is also supported") {
                test(
                    "i prefer describe/it but if there is no subject to describe I use " +
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

## gradle build

```kotlin
repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/christophsturm/maven/")
}

dependencies {
    testImplementation("failfast:failfast:0.1")
}
```

## running

Currently, there is no gradle plugin and no idea plugin. just create a main method in your test sources

```kotlin
fun main() {
    // this will find all tests that are in the same source root as TransactionFunctionalTest
    Suite.fromClasses(findTestClasses(TransactionFunctionalTest::class)).run().check()
}

```

add this to your gradle file:

```kotlin
val testMain =
    task("testMain", JavaExec::class) {
        main = "<my-package>.FailFastMainKt"
        classpath = sourceSets["test"].runtimeClasspath
    }

tasks.check { dependsOn(testMain) }
```

`./gradlew check` will then run the tests.

you can also skip test detection alltogether and just create a suite from a list of root contexts. Look at the test
suite for more info

## test lifecycle

Just declare your dependencies in the context blocks. they will be recreated for every test. it just works as expected.
I think ScalaTest has a mode that works like that and kotest also supports it,and calls
it  [instance per leaf](https://github.com/kotest/kotest/blob/master/doc/isolation_mode.md#instanceperleaf)

It combines the power of a dsl with the simplicity of junit 4.

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

## failfast?

It's pretty fast. its own test suite runs in less than one second:

```kotlin
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
expectThat(uptime).isLessThan(1000) // lets see how far we can get with one second
```


## autotest

add a autotest main method, and pass a random test class to it. the class is just used to get the test classloader.

```kotlin
fun main() {
    autoTest(anyTestClass = ContextExecutorTest::class)
}
```

create a gradle exec task for it:

```kotlin
task("autotest", JavaExec::class) {
    main = "failfast.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}
```

run it with `./gradlew -t autotest`anytime a test file is recompiled it will run. This works pretty well, but it's not
perfect, because not every change to a tested class triggers a recompile of the test class. Fixing this by reading
dependencies from the test classes' constant pool is on the road map.

## even faster tests, best practices

* avoid heavyweight dependencies. the failfast test suite runs in < 1000ms. that's a lot of time for a computer, and a
  great target for your test suite. slow tests are a code smell. An unexpected example for a heavyweight dependency is
  mockk, it takes about 2 seconds at first invocation.


* spin up slow dependencies at start-up in a separate thread (if you have to use them). for example this is the main
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

## avoiding global state

* if you need a web server run it on a random port.
* if you need a database create a db with a random name for each
  test. [like here](https://github.com/christophsturm/r2dbcfun/blob/main/src/test/kotlin/r2dbcfun/test/TestUtil.kt#L18)
  (or run the test in a transaction that you rollback at the end)
