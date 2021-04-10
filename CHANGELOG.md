# Changelog

All notable changes to this project will be documented in this file.

## 0.4.0 - "Doppeltest" 2021-04-02

### Added

- Mocking. Failfast now has its own mocking framework. It's very simple and can only mock interfaces, but it's much
  faster than mockk, and has a nice api. (Look in the kdoc of Mock.kt or MockTest.kt for more info)
- Allow declaring multiple root contexts in one class file. This is especially useful to create root contexts
  dynamically, for example to run the same test cases on multiple databases.

### Changed

- Classes that fit the failfast naming pattern *Test but contain no failfast context are now ignored. This makes it
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
- Duplicate contexts fail the suite. failfast always needed contexts and tests to have unique names. before duplicate
  tests would just be ignored, now this is detected
- Contexts with the same name as a test in the same context fail the suite.

### Fixed

- Fixed a threading bug that only appears on the OpenJ9 JDK.
 

