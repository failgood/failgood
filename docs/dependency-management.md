# Dependency Management in FailGood

This document explains the dependency management approach in FailGood, particularly regarding JUnit and Pitest integration.

## Single Project Structure

FailGood deliberately keeps JUnit support and Pitest support in the main project rather than separating them into separate subprojects. This design decision was made for the following reasons:

1. **Build Speed**: Keeping everything in a single project makes the build process faster and smaller. The ./ci script demonstrates how quickly the entire build process runs, executing formatting, tests, and checks in a single, efficient pipeline.

2. **Reduced Complexity**: A monolithic structure reduces the complexity of the build process and dependency management, making it easier to maintain and contribute to the project.

3. **Simplified Dependency Graph**: Users don't need to manage multiple dependencies for different features; they can simply include the main FailGood dependency.

## Optional Dependencies

While the project is kept as a single unit, certain dependencies are marked as optional:

### Pitest

Pitest is implemented as an optional dependency in FailGood:

- It's declared as a `compileOnly` dependency in the main source set, meaning it's not included in the transitive dependency graph.
- Users who want to use Pitest with FailGood need to explicitly include the Pitest dependency in their projects.
- The Pitest integration is isolated in the `failgood.pitest` package, ensuring it doesn't affect users who don't need this functionality.

### JUnit Platform

JUnit Platform integration is more tightly coupled with FailGood:

- JUnit Platform Commons and Launcher are included as API dependencies, making them part of the public API.
- JUnit Platform Engine is a `compileOnly` dependency, allowing for integration with the JUnit Platform but not forcing it on all users.
- Without JUnit integration, the test runner would have limited functionality, as it's designed to work within the JUnit ecosystem.
- However, the core testing functionality of FailGood could theoretically work without JUnit, even though this isn't the primary use case.

## Benefits of This Approach

This dependency management approach offers several benefits:

1. **Fast Builds**: The ./ci script demonstrates how quickly the entire build process runs, which would be slower with a multi-project setup.

2. **Small Footprint**: The final artifact remains small and focused, without unnecessary dependencies for users who don't need certain features.

3. **Flexibility**: Users can choose which optional features to include in their projects by adding the corresponding dependencies.

4. **Simplified Development**: Developers working on FailGood can easily navigate and modify the codebase without jumping between multiple subprojects.

## Conclusion

The decision to keep JUnit and Pitest support in the main project rather than separating them into subprojects is a deliberate design choice that prioritizes build speed, simplicity, and a small footprint. This approach has proven effective for FailGood's development and usage patterns.
