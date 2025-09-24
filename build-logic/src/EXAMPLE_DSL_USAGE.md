# DSL Configuration Examples

The build-logic now uses a flexible DSL that allows different projects to configure their build settings independently while sharing the same build-logic infrastructure.

## How the DSL Works

### 1. Core DSL Extensions

The DSL provides two main extension points:

- **CommonBuildExtension**: Configures JVM targets, Kotlin features, and power assert functions
- **PublishingBuildExtension**: Configures project metadata for publishing

### 2. Using the DSL in Projects

#### Example 1: Failgood Configuration (JVM 1.8 for production, strict mode)

```kotlin
// In failgood.defaults.gradle.kts
configure<CommonBuildExtension> {
    jvmTarget {
        production = "1.8"  // Support older JVMs
        test = 17
    }
    useFailgoodPowerAssert()  // Adds failgood.softly.AssertDSL.assert
    useStrictKotlinMode()     // Enables -XXexplicit-return-types=strict
}

configure<PublishingBuildExtension> {
    projectInfo {
        name = "FailGood"
        description = "a fast test runner for kotlin"
        url = "https://github.com/failgood/failgood"
    }
    scm {
        fromGitHub("failgood/failgood")
    }
}
```

#### Example 2: Isolation-Chamber Configuration (JVM 11 for production)

```kotlin
// In isolationchamber.common.gradle.kts
val commonBuild = extensions.create<CommonBuildExtension>("commonBuild", project)

commonBuild.apply {
    jvmTarget {
        production = 11  // Requires JVM 11+
        test = 17
    }
    useBasicPowerAssert()     // Only standard assert functions
    useStrictKotlinMode()     // Also uses strict mode
}
```

#### Example 3: Custom Project Configuration

```kotlin
// For a project that needs different settings
plugins {
    id("failgood.common")
}

configure<CommonBuildExtension> {
    jvmTarget {
        production = 21  // Latest JVM
        test = 21
    }
    useBasicPowerAssert()
}
```

## Benefits of This Approach

1. **Flexible Configuration**: Each project can choose its JVM targets independently
2. **Feature Opt-in**: Projects explicitly choose which features they want (strict mode, failgood assertions, etc.)
3. **Type-safe DSL**: IDE autocomplete and compile-time checking
4. **Shared Infrastructure**: All projects use the same underlying build-logic code
5. **Clear Intentions**: Configuration is self-documenting

## Migration Path

To use this in isolation-chamber or any other project:

1. Copy the `buildlogic` package to your build-logic
2. Update your convention plugins to use the DSL
3. Configure the settings according to your project needs

The DSL allows failgood to maintain JVM 1.8 compatibility for its library while isolation-chamber can require JVM 11, all while sharing the same build infrastructure.
