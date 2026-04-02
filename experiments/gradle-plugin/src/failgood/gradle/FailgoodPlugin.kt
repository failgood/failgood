package failgood.gradle

import java.net.URLClassLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.util.PatternFilterable
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener

class FailgoodPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        project.tasks.register("customTest", CustomTestTask::class.java) { task ->
            task.group = "verification"
            task.description = "Runs tests using the Failgood test engine"

            project.plugins.withType(JavaPlugin::class.java) {
                val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
                val testSourceSet =
                    javaExtension.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
                task.classpath = testSourceSet.runtimeClasspath
                task.testClassesDirs = testSourceSet.output.classesDirs
                task.dependsOn(testSourceSet.classesTaskName)
            }
        }
    }
}

open class CustomTestTask : DefaultTask() {
    @Input
    @Optional
    @Option(
        option = "tests",
        description = "Sets the test class or method to be included, '*' is supported.")
    var testFilter: String = "*"

    @org.gradle.api.tasks.Classpath lateinit var classpath: org.gradle.api.file.FileCollection

    @org.gradle.api.tasks.InputFiles
    lateinit var testClassesDirs: org.gradle.api.file.FileCollection

    @TaskAction
    fun runTests() {
        val testClasses = discoverTests()
        val filteredTests = filterTests(testClasses)

        logger.lifecycle("Running custom tests with filter: $testFilter")
        if (filteredTests.isEmpty()) {
            logger.lifecycle("No custom tests matched filter: $testFilter")
            return
        }

        executeTests(filteredTests)
    }

    private fun discoverTests(): List<String> {
        val tree: FileTree =
            testClassesDirs.asFileTree.matching { filter: PatternFilterable ->
                filter.include("**/*Test.class")
            }
        return tree.files.map { file ->
            file
                .toRelativeString(testClassesDirs.first())
                .removeSuffix(".class")
                .replace('/', '.')
                .replace('\\', '.')
        }
    }

    private fun filterTests(testClasses: List<String>): List<String> {
        return testClasses.filter { it.contains(testFilter.replace("*", "")) }
    }

    private fun executeTests(testClasses: List<String>) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader =
            URLClassLoader(
                classpath.files.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)

        classLoader.use { testClassLoader ->
            Thread.currentThread().contextClassLoader = testClassLoader
            try {
                val selectors =
                    testClasses.map { testClassName ->
                        DiscoverySelectors.selectClass(testClassLoader.loadClass(testClassName))
                    }
                val request = LauncherDiscoveryRequestBuilder.request().selectors(selectors).build()
                val launcher = LauncherFactory.create()
                val listener = SummaryGeneratingListener()

                launcher.execute(request, listener)

                val summary = listener.summary
                if (summary.testsFailedCount > 0 || summary.testsAbortedCount > 0) {
                    throw GradleException(
                        "Custom tests failed: ${summary.testsFailedCount} failed, " +
                            "${summary.testsAbortedCount} aborted.")
                }
            } finally {
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
        }
    }
}
