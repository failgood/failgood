### Failgood in 5 minutes

Here's your 5 minutes crash course for failgood.

first you need a [gradle](gradle.md) build file: (`/build.gradle.kts`)
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("dev.failgood:failgood:0.7.2")
}
tasks.test {
    useJUnitPlatform()
}
```

and a test (`/src/test/kotlin/MyFirstFailgoodTest.kt`)
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

Failgood searches tests in classes,objects or files that have the `failgood.Test` annotation.
This annotation also tell idea to display a run icon to run tests in the IDE.
If the run icon next to the class is missing you can also run the test by right clicking the classname.

![run popup.png](images/run%20popup.png)

This will show you the test results in a tree:
![test results.png](images/test%20results.png)

In this result tree you can jump to test sources, or run single tests or contexts.

Now start writing some tests or if you want to continue reading why not head here: [idea support](idea%20support.md)
