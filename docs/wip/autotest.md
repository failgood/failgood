## Autotest

This is an experimental feature from the early days of Failgood, but it still works.
Stay tuned for improved autotest support in the future or use the current version it like described below, and tell me what you think about it.

Add a main method that just runs autotest:

```kotlin
fun main() {
    autoTest()
}
```

create a gradle exec task for it:

```kotlin
tasks.register("autotest", JavaExec::class) {
    mainClass.set("failgood.AutoTestMainKt")
    classpath = sourceSets["test"].runtimeClasspath
}
```

run it with `./gradlew -t autotest`anytime a test file is recompiled it will run. This works pretty well, but it's not
perfect, because not every change to a tested class triggers a recompile of the test class. Fixing this by reading
dependencies from the test classes' constant pool is on the roadmap.

