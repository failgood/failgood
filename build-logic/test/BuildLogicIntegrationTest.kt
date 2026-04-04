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

        describe("pitest version configuration", isolation = false) {
            val defaultVersionProject =
                prepareTestProject("module-opt-ins").also {
                    addPitestVersionCatalog(it, "9.9.9")
                    addPitestVersionPrintTask(it)
                }
            val explicitVersionProject =
                prepareTestProject("module-opt-ins").also {
                    addPitestVersionCatalog(it, "9.9.9")
                    configureRootPitestVersion(it, "1.2.3")
                    addPitestVersionPrintTask(it)
                }

            val defaultVersionResult =
                gradleRunner(defaultVersionProject, ":pitest-enabled:printConfiguredPitestVersion")
                    .build()
            val explicitVersionResult =
                gradleRunner(
                        explicitVersionProject,
                        ":pitest-enabled:printConfiguredPitestVersion",
                    )
                    .build()

            test("pitest plugin version ignores version catalogs without root override") {
                assert(defaultVersionResult.output.contains("configuredPitestVersion="))
                assert(!defaultVersionResult.output.contains("configuredPitestVersion=9.9.9"))
            }

            test("pitest plugin version uses explicit root override") {
                assert(explicitVersionResult.output.contains("configuredPitestVersion=1.2.3"))
            }
        }

        describe("kmp inheritance", isolation = false) {
            val testProject = prepareTestProject("kmp-inheritance")
            val result =
                gradleRunner(
                        testProject,
                        ":inherited:printConfiguredTargets",
                        ":overridden:printConfiguredTargets",
                    )
                    .build()

            test("kmp modules inherit and override root JVM target defaults") {
                assert(result.output.contains("module=inherited mainJvmTarget=JVM_11 testJvmTarget=JVM_17"))
                assert(result.output.contains("module=overridden mainJvmTarget=JVM_17 testJvmTarget=JVM_17"))
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

private fun addPitestVersionPrintTask(projectDir: File) {
    val buildFile = File(projectDir, "pitest-enabled/build.gradle.kts")
    buildFile.appendText(
        """

tasks.register("printConfiguredPitestVersion") {
    doLast {
        val pitestExtension =
            project.extensions.getByType(
                info.solidsoft.gradle.pitest.PitestPluginExtension::class.java
            )
        println("configuredPitestVersion=${'$'}{pitestExtension.pitestVersion.orNull}")
    }
}
""".trimIndent()
    )
}

private fun configureRootPitestVersion(projectDir: File, version: String) {
    val buildFile = File(projectDir, "build.gradle.kts")
    buildFile.appendText(
        """

pitest {
    pitestVersion = "$version"
}
""".trimIndent()
    )
}

private fun addPitestVersionCatalog(projectDir: File, version: String) {
    val catalogFile = File(projectDir, "gradle/libs.versions.toml")
    catalogFile.parentFile.mkdirs()
    catalogFile.writeText(
        """
[versions]
pitest = "$version"
""".trimIndent()
    )
}
