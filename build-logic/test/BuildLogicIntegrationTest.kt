package buildlogic

import failgood.Test
import failgood.tests
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
                    .buildAndFail() // Expect the build to fail due to test failures

            val testOutput = extractTestTaskOutput(result.output)

            test("project should compile fine but tests should fail") {
                log(result.tasks.joinToString { it.path })
                assert(result.task(":core:compileTestKotlin")?.outcome == TaskOutcome.SUCCESS)
                assert(result.task(":core:test")?.outcome == TaskOutcome.FAILED)
            }

            test("power assert should augment standard asserts") {
                // Check that power assert is working by looking for its characteristic output
                // Power assert shows values aligned under the expression with | characters
                assert(testOutput.contains("assert(1 == 2)"))
            }
            test("power assert should augment failgood asserts") {
                // Check that power assert is working by looking for its characteristic output
                // Power assert shows values aligned under the expression with | characters
                assert(testOutput.contains("assert(1 == 3)"))
            }
        }

        describe("module opt-ins", isolation = false) {
            val testProject = prepareTestProject("module-opt-ins")

            val pitestResult = gradleRunner(testProject, "pitest", "--dry-run").build()
            val pomResult =
                gradleRunner(testProject, ":published:generatePomFileForMavenPublication").build()
            val missingPublicationResult =
                gradleRunner(testProject, ":plain:generatePomFileForMavenPublication")
                    .buildAndFail()
            val pomFile = File(testProject, "published/build/publications/maven/pom-default.xml")

            test("root pitest only schedules opted-in modules") {
                assert(pitestResult.output.contains(":pitest-enabled:pitest SKIPPED"))
                assert(!pitestResult.output.contains(":plain:pitest"))
                assert(!pitestResult.output.contains(":published:pitest"))
            }

            test("publishing stays opt-in") {
                assert(
                    missingPublicationResult.output
                        .lowercase()
                        .contains(
                            "task 'generatepomfileformavenpublication' not found in project ':plain'"
                        )
                )
            }

            test("published pom uses required default metadata") {
                assert(
                    pomResult.task(":published:generatePomFileForMavenPublication")?.outcome ==
                        TaskOutcome.SUCCESS
                )
                assert(pomFile.exists())
                val pom = pomFile.readText()
                assert(pom.contains("<name>Fixture Library</name>"))
                assert(pom.contains("<description>Fixture description</description>"))
                assert(pom.contains("<url>https://example.invalid/library</url>"))
                assert(pom.contains("<scm>"))
                assert(
                    pom.contains(
                        "<connection>scm:git:https://github.com/example/library.git</connection>"
                    )
                )
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

private fun extractTestTaskOutput(fullOutput: String): String {
    val lines = fullOutput.lines()
    val startIndex =
        lines.indexOfFirst { it.contains("Gradle Test Executor") && it.contains("STANDARD_OUT") }
    if (startIndex == -1) return fullOutput

    val endIndex =
        lines.indexOfFirst {
            it.contains("tests completed") || it.contains("Finished generating test")
        }

    return if (endIndex > startIndex) {
        lines.subList(startIndex, endIndex).joinToString("\n")
    } else {
        fullOutput
    }
}

private fun gradleRunner(projectDir: File, vararg arguments: String): GradleRunner {
    return GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*arguments, "--stacktrace")
        .withPluginClasspath()
}
