## Even faster tests - best practices


* avoid heavyweight dependencies.
  the Failgood test suite runs in < 1000ms. That's a lot of time for a computer, and a
  great target for your test suite. Slow tests are a code smell. An unexpected example for a heavyweight dependency is
  mockk, it takes about 2 seconds at first invocation. To avoid that you can use the simple mocking library that comes
  with Failgood. (see [MockTest.kt](../../failgood/src/test/kotlin/failgood/mock/MockTest.kt))


## Avoiding global state

Failgood runs your tests in parallel, so you need to avoid global state.
* if you need a web server run it on a random port.
* if you need a database create a db with a random name for each test. (see the.orm)
  or run the test in a transaction that is rolled back at the end

