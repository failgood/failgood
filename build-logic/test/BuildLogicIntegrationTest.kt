package buildlogic

import failgood.Test
import failgood.tests
import org.gradle.testkit.runner.BuildTask
import java.io.File
import kotlin.io.path.createTempDirectory
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

@Test
class BuildLogicIntegrationTest {
    val tests = tests {
        test("should build isolation-chamber-simplified multi-module project") {
            val testProject = prepareTestProject("isolation-chamber-simplified")

            val result =
                GradleRunner.create()
                    .withProjectDir(testProject)
                    .withArguments("build", "--stacktrace")
                    .withPluginClasspath()
                    .forwardOutput()
                    .build()

            // Check that no tasks failed
            val failedTasks = result.tasks.filter { it.outcome == TaskOutcome.FAILED }
            assert(failedTasks.isEmpty())

            // Assert on the exact list of build tasks that were executed
            val buildTasks =
                result.tasks.filter { it.path.endsWith(":build") }.map { it.path }.sorted()
            assert(
                buildTasks ==
                    listOf(":core:build", ":integresql:build", ":integresql-client:build").sorted()
            )
        }

        describe("power assert", isolation = false) {
            val testProject = prepareTestProject("power-assert-test")

            val result =
                GradleRunner.create()
                    .withProjectDir(testProject)
                    .withArguments("test", "--info")
                    .withPluginClasspath()
                    .forwardOutput()
                    .buildAndFail() // Expect the build to fail due to test failures
            test("project should compile fine but tests should fail") {
                log(result.tasks.joinToString { it.path })
                assert(result.task(":core:compileTestKotlin")?.outcome == TaskOutcome.SUCCESS)
                assert(result.task(":core:test")?.outcome == TaskOutcome.FAILED)
            }

            test("power assert should augment standard asserts") {
                val output = result.output

                // Check that power assert is working by looking for its characteristic output
                // Power assert shows values aligned under the expression with | characters
                assert(output.contains("assert(1 == 2)"))
            }
            test("power assert should augment failgood asserts") {
                val output = result.output

                // Check that power assert is working by looking for its characteristic output
                // Power assert shows values aligned under the expression with | characters
                assert(output.contains("assert(1 == 3)"))
            }
        }
    }
}

private fun prepareTestProject(projectName: String): File {
    val sourceDir =
        File(
            BuildLogicIntegrationTest::class
                .java
                .classLoader
                .getResource("test-projects/$projectName")
                ?.toURI() ?: throw IllegalArgumentException("Test project $projectName not found")
        )

    val tempDir = createTempDirectory("gradle-test").toFile()
    sourceDir.copyRecursively(tempDir)

    return tempDir
}
