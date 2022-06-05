### Troubleshooting

There should be no need for a troubleshooting section in the docs. If something does
not work as expected file an issue at https://github.com/failgood/failgood/issues or
ask for help in the #failgood channel in the kotlin-lang slack.

#### Reporting a problem

During test execution failgood collects all kinds of info that could be interesting to track down a bug.
If an exception is thrown this information will be logged together with the exception, so you can just copy paste
the whole error message including all stack traces when you want to report an error.
If there is a bug that does not trigger an exception, you can create a debug log file by setting `failgood.debug=true`
in `junit-platform.properties` (put it into src/test/resources)
FailGood will then write a file `failgood.debug.txt` that you can attach when you report a bug.

#### Migrating from older versions

Until Failgood reaches a version 1.0 there may be api changes that are not backwards compatible. Those should always be trivial to resolve.

##### Migrating to V0.6
If you get the error message:`One type argument expected for interface ContextDSL<GivenType>` just change from `ContextDSL` to `ContextDSL<*>`

