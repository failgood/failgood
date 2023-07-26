### Test Isolation

In failgood per default every test runs with a fresh copy of all its dependencies. For example in this example
every test will run with a separate web server, that is closed after the test runs.

```kotlin
//...
describe("my tests") {
    val server = WebServer().start(0/*random port*/)
    autoClose(server) // this could be part of the previous line but in a separate line here for simplicity
    it("can login") {
        server.login()
        assert(/*...*/)
    }
    it("returns 404 for not found requests") {
        assert(server.request(/*..*/))
    }
}
```

This is the default for failgood and you should always stick to that if you can.

#### Contexts without isolation

If all your dependencies are immutable you can turn off isolation for a context. Or another usecase would be to
do an action in a context and then assert on different aspects in separate tests.

```kotlin
describe("string reversing", isolation=false) {
    val string = "my String"
    val reversedString = string.reversed()
    it("produces a string that ends with the first character of the original string") {
        assert(reversedString.last() == string.first())
    }
    it("produces a string with the same length as the original") {
        assert(reversedString.length == string.length)
    }
}
```

This is a totally made up example, and in this example you could argue that turning off isolation is a premature optimisation, but you get the idea.

You can turn off isolation for a whole top level contexts, or also for subcontexts. If you don't specify an isolation parameter your context keeps the isolation level of its parent. Also, you can not turn on isolation once it is turned off because that would make no sense.


### If you really want shared state in your tests

If you use the default isolation but for some reason you want a dependency to not be recreated for every test, you can declare it outside the root context block, and if you have to close it, do it in an afterSuite callback.

```kotlin
class MyBeautifulTest {
    val myHeavyWeightTestDependency = KafkaDockerMegaMonolith()
    val context = describe("The web server") {
        afterSuite {
            myHeavyWeightTestDependency.close()
        }
        it("is fast") {
            // ...
        }
    }
}
```

This is really something you should rarely do. Instead, it's always better to isolate the shared dependency and create a fresh session for each test:

```kotlin

 import java.util.UUID
 // in KafkaTestUtil.kt

 class KafkaSession(val container: KafkaContainer, val topicPrefix: String) : AutoCloseable{
     /// ...
 }
object SharedKafka {
    val container = KafkaContainer()
    fun session() = KafkaSession(container, topicPrefix=UUID.randomUUID().toString())
}

// in the unit test
class MyKafkaSenderTest {
    val context = describe(KafkaSender::class) {
        val kafkaSession = autoClose(SharedKafka.session())
        val subject = KafkaSender(kafkaSession/*....*/)
        it("can send") {
        //...
        }
    }
}
```
