### Troubleshooting

There should be no need for a troubleshooting section in the docs. If something does
not work as expected file an issue at https://github.com/failgood/failgood/issues or
ask for help in the #failgood channel in the kotlin-lang slack.

#### Migrating from older versions

Until Failgood reaches a version 1.0 there may be api changes that are not backwards compatible. Those should always be trivial to resolve.

##### Migrating to V0.6
If you get the error message:`One type argument expected for interface ContextDSL<GivenType>` just change from `ContextDSL` to `ContextDSL<*>`
