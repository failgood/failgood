### Gradle build

Just add a Failgood dependency and configure gradle to use the Junit platform. Your build file could look like this:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.0")
}
tasks.test {
    useJUnitPlatform()
}
```

You can also configure [coverage plugins](coverage.md) in your gradle build

a full-fledged gradle with pitest, kover, kotlin-power-assert and test logging looks like this:

```kotlin
import failgood.versions.coroutinesVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension

/** A kotlin project that uses failgood as test runner and pitest for mutation coverage. */
plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("com.ncorti.ktfmt.gradle")
    kotlin("plugin.power-assert")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.9.2")
}
tasks.test {
    useJUnitPlatform()
}

configure<TestLoggerExtension> {
    theme = MOCHA_PARALLEL
    showSimpleNames = true
    showFullStackTraces = true
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result")) // filter out kotlin internal classes
        targetClasses.set(setOf("basepackage.*")) // by default "${project.group}.*"
        targetTests.set(setOf("basepackage.*Test", "basepackage.**.*Test"))
    }
}

```

### Running a subset of tests with gradle

You can run a single subcontext by setting the FAILGOOD_FILTER environment variable:
```
failgood % FAILGOOD_FILTER="The ContextExecutor > with a valid root context > executing all the tests" ./gradlew test
> Task :failgood:test

  The ContextExecutor > with a valid root context > executing all the tests ✔ returns tests in the same order as they are declared in the file
  The ContextExecutor > with a valid root context > executing all the tests ✔ returns contexts in the same order as they appear in the file
  The ContextExecutor > with a valid root context > executing all the tests ✔ returns deferred test results
  The ContextExecutor > with a valid root context > executing all the tests ✔ reports time of successful tests
  The ContextExecutor > with a valid root context > executing all the tests > reports failed tests ✔ reports exception for failed tests

  5 passing (1.3s)
```

just specify the test path like it is printed by the great com.adarshr.test-logger plugin.

This also works for single tests:

```
failgood % FAILGOOD_FILTER="The ContextExecutor > with a valid root context > executing all the tests > reports failed tests ✔ reports exception for failed tests" ./gradlew test

> Task :failgood:test

  The ContextExecutor > with a valid root context > executing all the tests > reports failed tests ✔ reports exception for failed tests

  1 passing (1.1s)
```
