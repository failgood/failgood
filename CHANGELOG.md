# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- color output in test report (robfletcher)
- a changelog
- Display a stacktrace entry that leads to the test method for failed tests.
- Report errors in contexts as failing tests.
- Tests are reported in the same order as they appear in the file.
- Contexts are reported in the same order as they appear in the file. who would have thought that they were reported in
  random order before.

### Changed

- Duplicate contexts fail the suite.
- Contexts with the same name as a test in the same context fail the suite.

### Fixed

- Fixed a threading bug that only appears on the OpenJ9 jdk.
 

