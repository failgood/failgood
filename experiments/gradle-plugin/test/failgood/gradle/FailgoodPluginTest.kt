package failgood.gradle

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FailgoodPluginTest {

    @TempDir lateinit var testProjectDir: File
    private lateinit var settingsFile: File
    private lateinit var buildFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        buildFile = File(testProjectDir, "build.gradle.kts")
    }

    private fun writeBuildFile() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
                plugins {
                    id("failgood.gradle.FailgoodPlugin")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
                    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
                }
            """
                .trimIndent())
    }

    private fun runner(vararg arguments: String): GradleRunner {
        return GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments(*arguments)
            .withPluginClasspath()
    }

    @Test
    fun `custom test task should run successfully`() {
        writeBuildFile()

        val testFile = File(testProjectDir, "src/test/java/SampleTest.java")
        testFile.parentFile.mkdirs()
        testFile.writeText(
            """
                import org.junit.jupiter.api.Test;

                class SampleTest {
                    @Test
                    void sampleTest() {
                        assert true;
                    }
                }
            """
                .trimIndent())

        val result = runner("customTest").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":customTest")?.outcome)
        assertTrue(result.output.contains("Running custom tests"))
    }

    @Test
    fun `custom test task should fail on before all error`() {
        writeBuildFile()

        val testFile = File(testProjectDir, "src/test/java/BrokenSetupTest.java")
        testFile.parentFile.mkdirs()
        testFile.writeText(
            """
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.Test;

                class BrokenSetupTest {
                    @BeforeAll
                    static void init() {
                        throw new RuntimeException("boom");
                    }

                    @Test
                    void sampleTest() {
                        assert true;
                    }
                }
            """
                .trimIndent())

        val result = runner("customTest").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":customTest")?.outcome)
        assertTrue(result.output.contains("Custom tests failed:"))
        assertTrue(result.output.contains("1 containers failed"))
    }
}
