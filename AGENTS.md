# Repository Guidelines

## for idiots:
run git and gradle commands elevated and do not run them in parallel

## Project Structure & Module Organization
- This is a multi-module Gradle/Kotlin repository.
- `failgood/` contains the core library. Sources are in `failgood/src`, tests in `failgood/test`, and test resources in `failgood/testResources`.
- `failgood-examples/` contains example test suites and usage patterns.
- `failgood-gradle-test/` holds Gradle Tooling API integration tests.
- `experiments/` contains optional modules (`failgood-debugger`, `gradle-plugin`, `pitest-parser`).
- `docs/` stores end-user and contributor documentation.


## Build, Test, and Development Commands
- `./gradlew check`: run formatting checks and tests for the selected task graph.
- `./gradlew ci`: run the same verification set used by CI across core modules.
- `./ci`: local pre-PR helper; runs `ktfmtFormat`, `./gradlew ci`, then `:failgood:pitest`, and writes parsed stats to `buildStats/`.
- `./gradlew koverXmlReport`: generate coverage XML at `failgood/build/reports/kover/xml/report.xml`.
- `./gradlew :failgood:testMain`: run the bootstrap-style suite from `failgood/test`.

## Coding Style & Naming Conventions
- Kotlin and Gradle Kotlin DSL are the standard.
- Follow `.editorconfig`: UTF-8, LF, 4-space indentation, max line length 120.
- Formatting is enforced with `ktfmt` (Kotlin style, no trailing commas).
- Use `PascalCase` for types and `camelCase` for functions/properties.
- Keep package names aligned with folder paths (for example `failgood/internal/execution`).
- Name tests clearly; most test files use the `*Test.kt` suffix.

## Testing Guidelines
- All modules run tests on JUnit Platform (`useJUnitPlatform()`).
- Place tests in each module’s `test/` directory and keep them deterministic/parallel-safe.
- For substantial changes, run `./ci` locally to include mutation testing (`:failgood:pitest`).
- For focused runs, use task filters, for example:
  `./gradlew :failgood:test --tests 'failgood.internal.*'`.

## Commit & Pull Request Guidelines
- Use short, imperative commit subjects, consistent with history (for example `fix ktfmt deprecation`).
- Keep commits scoped to one logical change and include tests when behavior changes.
- PR descriptions should cover: problem, approach, affected modules, and any docs/DSL updates.
- Ensure `./ci` passes before opening a PR; GitHub CI also validates on JDK 17 and 21.
