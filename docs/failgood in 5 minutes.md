### Failgood in 5 minutes

Here's your 5 minutes crash course for failgood.

first you need a [gradle](gradle.md) build file: (`/build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("dev.failgood:failgood:0.7.ß")
}
tasks.test {
    useJUnitPlatform()
}
```

and a test (`/src/test/kotlin/MyTest.kt`)
```kotlin
package failgood.examples

import failgood.Test
import failgood.describe

@Test
class MyFirstFailgoodTests {
    val context = describe("my perfect test suite") {
        it("runs super fast") {
            assert(true)
        }
        describe("tests can be organized in subcontexts") {
            it("just works") {}
        }
    }
}
```

Failgood detects that something contains tests by the failgood.Test annotation.
This annotation also tell idea to display a run icon to run tests in the IDE
