# Failfast
a simple and fast test runner for kotlin

## why?
* rspec like syntax implemented to work without any pitfalls.
* really fast (own test suite runs in < 1 second)
* really simple. no dependencies and not a lot of code
* it's the perfect test runner if you know what you are doing and care about the speed of your tests.


## how it looks
```
object FailfastTest {
    val context =
        describe("The test runner") {
            it("supports describe/it syntax") {}
            describe("nested contexts") {
                it("can contain tests too") {}
                itWill("itWill can be used to mark pending tests") {}
                itWill("for pending tests the test body is optional")
                context("context/test syntax is also supported") {
                    test("i prefer describe/it but if there is no subject to describe I use context/test") {
                    }
                    test("tests without body are pending")
                }
            }
        }
}

* The test runner
 - supports describe/it syntax (5.84ms)
 * nested contexts
  - for pending tests the test body is optional PENDING
  - can contain tests too (5.89ms)
  - itWill can be used to mark pending tests PENDING
  * context/test syntax is also supported
   - tests without body are pending PENDING
   - i prefer describe/it but if there is no subject to describe I use context/test (5.93ms)

```

## test lifecycle


```
* test dependencies
 - are recreated for each test

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
                        listOf(ROOT_CONTEXT_EXECUTED, CONTEXT_1_EXECUTED, CONTEXT_2_EXECUTED, TEST_3_EXECUTED, DEPENDENCY_CLOSED),
                        listOf(ROOT_CONTEXT_EXECUTED, TEST_4_EXECUTED, DEPENDENCY_CLOSED)
                    )
            }
        }
```
Just declare your dependencies in the context blocks. they will be recreated for every test. it just works as expected.
[kotest also supports this and calls it instance per leaf](https://github.com/kotest/kotest/blob/master/doc/isolation_mode.md#instanceperleaf)
kotest currently does not support parallel execution of test with this mode. 

## failfast?

It's pretty fast. its own test suite runs in less than one second:
```
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    expectThat(uptime).isLessThan(1000) // lets see how far we can get with one second
```


## running
Currently there is no gradle plugin and no idea plugin. 

just create a main method in your test sources
```
fun main() {
    Suite.fromClasses(findTestClasses(TransactionFunctionalTest::class)).run().check()
}


```
add this to your gradle file:
```
val testMain =
    task("testMain", JavaExec::class) {
        main = "<my-package>.FailFastMainKt"
        classpath = sourceSets["test"].runtimeClasspath
    }

tasks.check { dependsOn(testMain) }
```

## autotest
add a autotest main method, and pass a random test class to it. 
```
fun main() {
    autoTest(anyTestClass = ContextExecutorTest::class)
}
```
create a gradle exec task for it:
```
task("autotest", JavaExec::class) {
    main = "failfast.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}
```

run it with `./gradlew -t autotest`
anytime a test file is recompiled it will run. 

## even faster tests, best practices
* avoid heavy weight dependencies
  the failfast test suite runs in < 1000ms. that's a lot of time for a computer, and a great target
  for your test suite. slow tests are a code smell. 
  
* spin up slow dependencies at start-up in a separate thread (if you have to).
  for example this is the main test method of another open source project of mine:
  
```
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
* if you need a database create a db with a random name  for each test. [like here](https://github.com/christophsturm/r2dbcfun/blob/main/src/test/kotlin/r2dbcfun/test/TestUtil.kt#L18)
  (or run the test in a transaction that you rollback at the end)
