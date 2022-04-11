## How to write tests with Failgood

### setUp / beforeEach

If you are used to junit you probably wonder where to place init code that you want to run before each test.
Failgood has no setUp or beforeEach, because it is not a good fit for kotlin's immutable `val`s.

Tests in other test runners sometimes look like this:
```kotlin
class MyTest {
    private lateinit var myWebserver: Server

    @BeforeEach
    fun setUp() {
        myWebserver = Server()
    }

    @AfterEach
    fun tearDown() {
        myWebserver.close()
    }
}

```
In Failgood you just start your dependencies where you declare them, and define a callback to close them, so the Failgood
equivalent of the above code is just:

```kotlin
val context = describe(MyServer::class) {
    val myWebserver = autoClose(Server()) {it.close()}
}

```

Test dependencies will be recreated for every test. It just works as expected. Failgood executes the context block again for each test to have separate instances of all test dependencies.

