### The Philosophy of failgood, or how to write really stable software

##### Avoid configuration
One way how failgood tries to be the most stable test runner is by avoiding configuration.
Parallel test execution is not an optional feature, it is the default, and it's the only way to run tests. Parallel execution means everything runs in parallel, starting from the investigation of test context to the execution of individual tests. Also, the default is to run each test-case with a fresh instance of all of its dependencies. This is actually something you can override if you must, but in 99% of cases you should not do it.
Long story short: There is (almost) no configuration. Every failgood user runs the exact same code. Test isolation and parallelism is not an option that is "experimental" or "try at your own risk". It's the only way. And its super stable.

#### One way to do things
This is very related to the previous paragraph. There is just one test dsl, and for most things that you need to do there is just one way to do it. For example there is no `beforeEach` callback because for every use-case of beforeEach there is a [better way](how%20to%20write%20tests%20with%20failgood.md) to achieve the same.

##### TDD
Failgood is developed mostly test driven. Why mostly? Usually I'm a very strict TDDer, but for failgood
I sometimes add features just with a functional test and without unit test coverage for every branch. (Currently failgood has about 80% code coverage). But there is one strict rule: Everything that breaks will only be fixed after writing a test for it. Actually that's a rule I really like. If something is important enough that it can break it is also important enough to have a test.

##### Be Nice
Failgood should always produce a nice error message when something goes wrong. Test failures must look useful. There should never be a stacktrace from deep inside failgood that leaves you puzzling. There should be no "troubleshooting" section in the docs, no information that you just have to know. Everything that could be in a troubleshooting section should instead happen automatically. It's not Oracle or Gradle, there should be no way to be a failgood specialist, and there should be no reason to hire a consultant to help you with failgood. Of course writing good tests is still an art, and you can be a specialist in TDD. But the failgood part will always be boring and simple.
Only one jar file, no dependencies, very lenient with kotlin dependencies. No need for a BOM, no need
to upgrade to the latest kotlin version. Failgood will always just work. And if it does not, file a bug or ask in the #failgood channel in the kotlin slack, and we will fix it. If someone asks a question about failgood we will try to make that question obsolete.
