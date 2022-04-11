### Failgood in 5 minutes

Here's your 5 minutes crash course for failgood.

first you need a [gradle](gradle.md) build file: (`/build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("dev.failgood:failgood:0.6.1")
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
class MyFirstFailgoodTest {
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

Failgood finds tests by class name. When you run all test in the project or all tests in a package, every class that ends with `Test` is considered to contain tests.
The `failgood.Test` annotations is there to tell IDEA that this is a class that contains tests that it should offer to run.

You can run a single test class from idea even if it's not named *Test, but it's recommended to just keep it simple and name all your test classes *Test
