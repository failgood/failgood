package failgood.gradle

import failgood.Test
import failgood.describe
import org.gradle.tooling.GradleConnector
import java.io.File

@Test
class GradleTest {
    val context = describe("running via gradle") {
        it("works") {
            val buildDir = File(
                GradleTest::class.java.getResource("/simple-gradle-project/settings.gradle.kts")!!.toURI()
            ).parentFile
            GradleConnector.newConnector()
                .forProjectDirectory(buildDir)
                .connect().use { connection ->
                    connection.newBuild().forTasks("tasks").run()
                }
        }
    }
}
