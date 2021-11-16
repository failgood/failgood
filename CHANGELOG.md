
# Changelog

All notable changes to this project will be documented in this file.

## 0.5.0 - "Reinforced" - Unreleased



### Changed

- Use the same timeout for context evaluation and test execution (configurable via env var TIMEOUT in milliseconds) (#32)
- Report timeouts in context evaluation as Failed Context instead of throwing and failing the whole suite. (#36)

## 0.4.9 - "No U-Turn" - 2021-11-16

### Fixed

- Correctly report errors in root contexts even in gradle. (#34)



## 0.4.8 - "Good Looking" - 2021-10-29

### Changed

- bump timeout to 40 seconds for now.

### Added

- Output UniqueIds of failed tests in junit engine runner to make it possible to re-run them in IDEA.

### Fixed

- Fix some run-tests-by-uniqueId edge cases

## 0.4.7 - "Let's try this" - 2021-09-17

### Added

- Support UniqueId junit selectors to run single tests or subcontexts. This already works in idea if you select uniqueId
  in the run config and enter the uniqueId selector like
  this: `[engine:failgood]/[class:The+ContextExecutor(failgood.internal.ContextExecutorTest)]/[method:with+a+valid+root+context]/[method:reports+line+numbers]` (
  with + instead of space). Now idea just needs to do this
  automatically:https://youtrack.jetbrains.com/issue/IDEA-277855

### Fixed

- Add a runtimeOnly dependency on junit launcher to make it easier to run test in idea.
- Correctly handle the case where duplicate root contexts contain tests with the same name.

## 0.4.6 - "Ok, Gradle" 2021-09-02

### Changed

- Improve running the tests in Gradle. It works pretty well now.
- Report failures in root contexts instead of throwing and failing the whole suite
- Introduce @Test annotation to replace junit platforms @Testable. This will probably change again before 0.5.0
- Mocks: Replace `thenReturn(result)` with `then {result}`, and allow mock methods to throw exceptions

## 0.4.5 - "Being Boring" - 2021-08-09

### Changed

- Use kotlin 1.5.21. Coroutines 1.5.1

### Fixed

- Support mocking functions with a nullable return type.
- Duplicate root context names no longer break the junit engine. It's still probably a good idea to give your root
  contexts unique names. But the JUnit engine will now work even if you name all your root contexts RootContext. (or
  Rudolph)

## 0.4.4 - "Past Perfect" - 2021-06-15

### Changed

- `FailGood.runTest()` and `FailGood.runAllTests()` are now `suspend`, so you have to make your test main
  functions `suspend` too.

### Added

- Allow to override the context timeout by setting the env variable TIMEOUT. Set it to any millisecond value to change
  it or to something that is not a number to disable timeouts.
- Allow ordering of contexts by passing an order value. contexts with lower order will be executed first.
- `afterSuite {}` method that is called after all tests have finished.

### Fixed

- Fix println which was not doing anything in the JUnit Platform engine.

## 0.4.3 - "FailGood" - 2021-05-21

### Fixed

- Fix stack overflow when running a single test that uses println.

### Changes

- Change the project name to FailGood.

## 0.4.2 - "Truth" - 2021-05-14

### Added

- Run a single test class from idea even if it is not called *Test if it's annotated with @Testable
- Support declaring test contexts in classes. IDE support for tests in classes is just better. You can now add a
  @Testable annotation to your test class and run a single class from idea. The class is only instantiated once, so
  don't put dependencies into the class outside a context.

### Fixed

- Correctly report failing contexts in the JUnit platform engine.

## 0.4.1 - "Polymerase" - 2021-04-29

### Added

- Support autoClose inside tests.
- Add autoClose(AutoCloseable) to close AutoCloseables without specifying a close callback.
- Mocking library `call(..)` helper now supports suspend functions
- `println` method inside a test case for thread safe test logging. Currently, the output is only available in the JUnit
  platform engine, The main test runner does not yet display it.
- Use DSLMarker to ensure that tests do not create contexts. This is never what you want, so failgood now protects you
  from it. Here is an example:
  ```kotlin
                  test("tests can not contain nested contexts") {
                    context("this should not even compile") {} // compile error
                }

  ```

## 0.4.0 - "Doppeltest" - 2021-04-02

### Added

- Mocking. Failfast now has its own mocking framework. It's very simple and can only mock interfaces, but it's much
  faster than mockk, and has a nice api. (Look in the kdoc of Mock.kt or MockTest.kt for more info)
- Allow declaring multiple root contexts in one class file. This is especially useful to create root contexts
  dynamically, for example to run the same test cases on multiple databases.

### Changed

- Classes that fit the failgood naming pattern *Test but contain no failgood context are now ignored. This makes it
  possible to convert bigger test suites that still use other frameworks without converting everything at once.
- Rename itWill to pending

### Fixed

- AutoCloseables were closed in the same order they were created. Now they are correctly closed in reverse order.
- Report individual Test duration correctly. Before the first test in each context had a wrong time reported.

## 0.3.1 - "Zwischenhaltestelle" - 2021-03-05

### Changed

- Make autoClose callback method suspend for easier closing of resources that need coroutines.

### Added

- Output tests/sec in test summary.

### Fixed

- Correctly report failures in contexts, even when the first context run is successful.
- Send test start and stop events in correct order
- Always report contexts as Successful, as requested by the Junit Platform API

## 0.3.0 - "Zuckerwattestand" - 2021-02-21

### Added

- JUnit Platform Engine for easy and comfortable test running from IDEA. It's new and experimental, but it works pretty
  well. For some suites it can take pretty long for the test tree to appear, this will be improved soon.
- Print each context result as soon as the context is finished.
- Display test failures in a more compact way. Stack traces are shortened to only show lines from the test, and the
  exception name is not added to the output when it is an assertion error. Those usually have an error message that
  speaks for itself
- Color output in test report (robfletcher)
- A changelog
- Display a stacktrace entry that leads to the test method for failed tests.
- Report errors in contexts as failing tests.
- Tests are reported in the same order as they appear in the file.
- Contexts are reported in the same order as they appear in the file. who would have thought that they were reported in
  random order before.

### Changed

- Publish to maven central. goodbye jcenter, you will be missed
- Don't output slow tests and pending tests separately for now. need to find a more useful way of showing this
- Duplicate contexts fail the suite. failgood always needed contexts and tests to have unique names. before duplicate
  tests would just be ignored, now this is detected
- Contexts with the same name as a test in the same context fail the suite.

### Fixed

- Fixed a threading bug that only appears on the OpenJ9 JDK.


