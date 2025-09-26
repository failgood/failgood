package buildlogic

import failgood.Test
import failgood.tests
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory

@Test
class BuildLogicIntegrationTest {
    val tests = tests {
        test("should build isolation-chamber-simplified multi-module project") {
                val testProject = prepareTestProject("isolation-chamber-simplified")

                val result = GradleRunner.create()
                    .withProjectDir(testProject)
                    .withArguments("build", "--stacktrace")
                    .withPluginClasspath()
                    .forwardOutput()
                    .build()

                // Check that no tasks failed
                val failedTasks = result.tasks.filter { it.outcome == TaskOutcome.FAILED }
                assert(failedTasks.isEmpty())

                // Assert on the exact list of build tasks that were executed
                val buildTasks = result.tasks.filter { it.path.endsWith(":build") }.map { it.path }.sorted()
                assert(buildTasks == listOf(":core:build", ":integresql:build", ":integresql-client:build").sorted())
        }

    }
}

private fun prepareTestProject(projectName: String): File {
        val sourceDir = File(BuildLogicIntegrationTest::class.java.classLoader
            .getResource("test-projects/$projectName")?.toURI()
            ?: throw IllegalArgumentException("Test project $projectName not found"))

        val tempDir = createTempDirectory("gradle-test").toFile()
        sourceDir.copyRecursively(tempDir)

        return tempDir
}
