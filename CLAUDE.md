# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FailGood is a test runner for Kotlin focusing on simplicity, usability, and speed. The main features include:

- Fast parallel test execution
- Simple test DSL in pure Kotlin
- No configuration - everything runs in parallel by default
- Good isolation - tests run with fresh instances of dependencies
- IntelliJ IDEA integration
- Gradle integration
- Compatible with various assertion libraries

## Build and Test Commands

### Running Tests

The primary way to run all tests is using the `./ci` script:

```bash
./ci
```

This script:
1. Formats the code using ktfmtFormat
2. Runs the Gradle `ci` task (which runs both regression tests and all regular tests)

For development, you can also run specific test tasks:

```bash
# Run FailGood's main test suite
./gradlew :failgood:testMain

# Run multi-threading performance tests
./gradlew :failgood:multiThreadedTest

# Auto-test (watches for changes and runs tests)
./gradlew :failgood:autotest

# Run just the CI tests
./gradlew ci
```

### Running in IntelliJ IDEA

To run tests in IDEA, run the `FailGoodBootstrap.kt` class or right-click on any test class with the `@Test` annotation.

## Test Structure

Tests are defined in classes with the `@Test` annotation:

```kotlin
@Test
class MyTest {
    val tests = testCollection {
        it("should do something") {
            // test code
        }
        
        describe("some context") {
            it("should handle a specific case") {
                // test code
            }
        }
    }
}
```

## Resource Management

Resources are created inline and disposed automatically:

```kotlin
val tests = describe(MyServer::class) {
    val myWebserver = autoClose(Server()) { it.close() }
    
    it("should handle requests") {
        // test using myWebserver
    }
}
```

## Test Isolation

Each test runs with its own instance of dependencies. The context block is executed again for each test to ensure proper isolation.

## Parameterized Tests

Use Kotlin's standard library functions for parameterized tests:

```kotlin
(1..5).forEach { value ->
    it("works with $value") {
        // test with value
    }
}
```