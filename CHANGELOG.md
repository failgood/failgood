
# Changelog

All notable changes to this project will be documented in this file.

## 0.8.3 " Beggars Banquet" - 2022-11-29

Ok another bugfix release before 0.9

### Fixed

- don't report ignored tests that should not run (#160)
- ignore tests that can not be instantiated in pitest plugin

## 0.8.2 "Blast First" - 2022-10-02

This version will be the last before 0.9 where all deprecated methods will be removed.
0.9.x versions will be beta/release candidate versions for 1.0

### Changed

- Sub-contexts can now also be ignored
- New Ignore API (#95,#63,139)

### Fixed

- AssertHelpers.containsExactlyInAnyOrder works when elements are not Comparable
- Handle exceptions in dependency block as result. (#130)

## 0.8.1 "Rather Interesting" - 2022-09-13

### Fixed

- Don't crash when root context is flaky.
- Correctly handle root context timeouts. the timeout should be for one test execution that is part of the Root Context
  and not for all the tests that are executed as part of the root context.

## 0.8.0 "Position Chrome" - 2022-08-29

### Added

- allow to disable isolation for sub-contexts.

### Fixed

- fix running tests by uniqueid where the root context contains ( or ) (#109)
- handle exceptions in afterEach after a test failure (fixes #110)
- context silently ignored when using describe<Class> for subcontexts. (#104)
- Running tests using FailGood::runAllTests or using the auto-test feature fails on Windows (#114)


## 0.7.2 "Rough Trade" - 2022-07-22

### Added

- Run single tests from gradle (#96)
- Collect Events that happen during test execution and add them to an exception in case of error.
  Or print it to a file when debug is set.


## 0.7.1 - "Ariola" - 2022-05-03

### Fixed

- Failgood incorrectly reports error when only a single test is selected that is not a failgood test (#93)


## 0.7.0 - "Universal" - 2022-04-29

### Changed

- Lots of changes to the previously undocumented mock library.
- Don't rely on class name patterns and instead run every class with a failgood.Test annotation

## 0.6.1 - "Blue Note" - 2022-04-06

### Added

- Support afterEach callbacks that run after each test.
- Integrate kover plugin, publish code coverage to codecov

### Fixed

- fix that some test names would not be found via uniqueid
- Correctly handle tags on tests created with `it`

## 0.6.0 - "Stones Throw" - 2022-02-21

### Changed

- Print a useful error message when test are not found because the test names change. (#61)

### Added

- Add support for given. Contexts can now define a lambda whose result is passed to tests
- Experimental run tests by tag. describe,it,test and context now have a `tags` parameter, and you can run a subset of tests
  by setting the env variable FAILGOOD_TAG. Currently, only contexts that are in the root can be tagged, tags in subcontexts
  of subcontexts are currently not found
- When a test fails print its uniqueId for easy re-running of that test from idea
  (just create a junit run config that runs by uniqueId)
- Issue a warning when tests are created in the wrong context because a method has the wrong receiver
- Catch errors in the junit engine, and log them to stdout. Tell users to submit an issue
- Print slowest test after test run when env var PRINT_SLOWEST_TESTS is set in the junit engine

### Fixed

- Treat test timeouts as failed test instead of throwing and failing the whole suite (#73)
- ignore errors in failed contexts (fixes #38)
- Errors in close callbacks were not caught correctly after a test failure (#65)

## 0.5.3 - "Warp" - 2022-02-01

### Fixed

- Correctly handle errors in contexts and root context, for example duplicate names (#58)


## 0.5.2 - "Fiction" - 2022-01-17

### Fixed

- Report exceptions in autoClose callbacks as test failure (#53)

## 0.5.1 - "Grand Royal" - 2022-01-14

### Fixed

- don't throw an error when the junit engine finds no tests, to allow other engines to run their tests. (#48)

### Added

- add support for package filtering. this fixes running all tests in a package from IDEA. (#50)
- support contexts defined in other variables than `context`. This also enables multiple contexts in one class. (#49)

## 0.5.0 - "Reinforced" - 2021-11-29

### Added

- Added support for root contexts without isolation. (experimental, API will probably change)

### Fixed

- Call autoClose tear down methods for failed tests too. Fascinating that this huge bug was undetected so long. #43

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


