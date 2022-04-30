### Gradle build

Just add a Failgood dependency and configure gradle to use the Junit platform. Your build file could look like this:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    testImplementation("dev.failgood:failgood:0.7.0")
}
tasks.test {
    useJUnitPlatform()
}
```

You can also configure [coverage plugins](coverage.md) in your gradle build

a full-fledged gradle with pitest, kover, kotlin-power-assert and test logging looks like this:

```kotlin
import info.solidsoft.gradle.pitest.PitestPluginExtension
import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL

plugins {
    kotlin("jvm") version "1.6.21"
    id("com.bnorm.power.kotlin-power-assert") version "0.11.0"
    id("org.jetbrains.kotlinx.kover") version "0.5.0"
    id("com.adarshr.test-logger") version "3.2.0"
    id("info.solidsoft.pitest")
}

dependencies {
    testImplementation("dev.failgood:failgood:0.7.0")
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
