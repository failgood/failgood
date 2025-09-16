package failgood.gradle

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
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

    //    @Test
    fun `custom test task should run successfully`() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
                plugins {
                    id("com.your.package.custom-test-plugin")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
                    testImplementation("your.failogood.dependency:failogood:version")
                }
            """
                .trimIndent())

        // Create a test file
        val testFile = File(testProjectDir, "src/test/kotlin/SampleTest.kt")
        testFile.parentFile.mkdirs()
        testFile.writeText(
            """
                import org.junit.jupiter.api.Test

                class SampleTest {
                    @Test
                    fun `sample test`() {
                        assert(true)
                    }
                }
            """
                .trimIndent())

        val result =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("customTest")
                .withPluginClasspath()
                .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":customTest")?.outcome)
        assertTrue(result.output.contains("Running custom tests"))
    }
}
