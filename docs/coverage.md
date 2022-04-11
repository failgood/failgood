## Test coverage

### Pitest

Pitest is the gold standard for test coverage. Failgood has great support for it. If your test suite
it fast and lightweight enough you shoud really use it.
Just use the [pitest gradle plugin](https://gradle-pitest-plugin.solidsoft.info) and a config like this:

```kotlin
plugins {
    // ...
    id("info.solidsoft.pitest")
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // useful for CI
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result")) // filter out kotlin internal classes
        targetClasses.set(setOf("failgood.*")) // by default "${project.group}.*"
        targetTests.set(setOf("failgood.*Test", "failgood.**.*Test"))
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        ) // useful to limit threads on CI
        outputFormats.set(setOf("XML", "HTML"))
    }
}

```

### Kover

Failgood also works well with the [kover](https://github.com/Kotlin/kotlinx-kover) plugin.
Just put it into your gradle plugins block and everything should just work.
```kotlin
plugins {
    id("org.jetbrains.kotlinx.kover") version "0.5.0"
}
```
