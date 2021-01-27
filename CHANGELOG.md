# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- a changelog
- Display a stacktrace entry that leads to the test method for failed tests.
- Report errors in contexts as failing tests.

### Changed

- Duplicate contexts fail the suite.
- Contexts with the same name as a test in the same context fail the suite.
- Tests are reported in the same order as they appear in the file.

### Fixed

- Fixed a threading bug that only appears on the OpenJ9 jdk.
 

