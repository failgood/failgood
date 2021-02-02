# Changelog

All notable changes to this project will be documented in this file.

## 0.3.0 [Unreleased]

### Added

- Junit Platform Engine for easy and comfortable test running from IDEA.
- Print each context result as soon as the context is finished.
- Display test failures in a more compact way. Stack traces are shortened to only show lines from the test. the
  exception name is not added to the output when it is an assertion error. those usually have a error message that
  speaks for itself
- Color output in test report (robfletcher)
- A changelog
- Display a stacktrace entry that leads to the test method for failed tests.
- Report errors in contexts as failing tests.
- Tests are reported in the same order as they appear in the file.
- Contexts are reported in the same order as they appear in the file. who would have thought that they were reported in
  random order before.

### Changed

- don't output slow tests and pending tests separately for now. need to find a more useful way of showing this
- Duplicate contexts fail the suite. failfast always needed contexts and tests to have unique names. before duplicate
  tests would just be ignored, now this is detected
- Contexts with the same name as a test in the same context fail the suite.

### Fixed

- Fixed a threading bug that only appears on the OpenJ9 jdk.
 

