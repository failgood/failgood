### If you really want shared state in your tests
If for some reason you want a dependency to not be recreated for every test, just declare it outside the root context block, and if you have to close it, do it in an afterSuite callback.

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
