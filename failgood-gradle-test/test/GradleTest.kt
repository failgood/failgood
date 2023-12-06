package failgood.gradle

import failgood.Ignored
import failgood.Test
import failgood.describe
import java.io.File
import kotlin.io.path.createTempDirectory
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.events.ProgressEvent
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.test.JvmTestOperationDescriptor
import org.gradle.tooling.events.test.internal.DefaultTestStartEvent

@Test
class GradleTest {
    val context =
        describe("running via gradle", ignored= Ignored.Because("does not in amper version")) {
            it("works") {
                val thisSource = File(GradleTest::class.java.protectionDomain.codeSource.location.toURI())
                val rootDirectory =
                    thisSource
                        .parentFile
                        .parentFile
                        .parentFile
                        .parentFile
                        .parentFile
                        .parentFile

                val buildDir = File(rootDirectory, "simple-gradle-project")
                val tempDir = createTempDirectory()
                buildDir.copyRecursively(tempDir.toFile())
                GradleConnector.newConnector()
                    .forProjectDirectory(tempDir.toFile())
                    .connect()
                    .use { connection ->
                        connection
                            .newTestLauncher()
                            .withJvmTestClasses("failgood.gradle.test")
                            .addProgressListener(Listener())
                            .run()
                    }
            }
        }
}

class Listener : ProgressListener {
    override fun statusChanged(event: ProgressEvent) {
        when (event) {
            is DefaultTestStartEvent -> {
                when (val d = event.descriptor) {
                    is JvmTestOperationDescriptor -> {
                        println("$event $d")
                    }
                }
            }
        }
        //        println("${event::class.java}-$event")
    }
}
