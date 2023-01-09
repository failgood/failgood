## Contributing

Failgood is still pretty new, but if you want to contribute that would be great.

### Running the Failgood test suite

To run FailGood's test suite just run `./gradlew check` or if you want to run it via idea just run
the `FailGoodBootstrap.kt` class.

Before committing something you can run `./ci` to reformat the source-code and run the same tests that will run on CI.

### Using a local checkout of failgood in your project

When trying out a new feature or bug fix you may want to use the git version of failgood instead of a release.
To do this just add `includeBuild(<PATH_TO_FAILGOOD_ROOT_DIR>)` to your projects `settings.gradle.kts` file.

For example if you have failgood checked out in the same directory level as your project this would be
`includeBuild("../failgood")`

Trying out the failgood main branch is very helpful for the failgood project. Make sure to report any issues you find in slack, or as GitHub issues if you think it is a bug.
