package buildgood

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class PublishingBuildExtension @Inject constructor(
    private val objects: ObjectFactory
) {
    val projectInfo = objects.newInstance(ProjectInfo::class.java)
    val scm = objects.newInstance(ScmConfig::class.java)

    fun projectInfo(action: Action<ProjectInfo>) {
        action.execute(projectInfo)
    }

    fun scm(action: Action<ScmConfig>) {
        action.execute(scm)
    }
}

open class ProjectInfo @Inject constructor() {
    var name: String? = null
    var description: String? = null
    var url: String? = null
}

open class ScmConfig @Inject constructor() {
    var connection: String? = null
    var developerConnection: String? = null
    var url: String? = null

    fun fromGitHub(repo: String) {
        connection = "scm:git:https://github.com/$repo.git"
        developerConnection = "scm:git:git@github.com:$repo.git"
        url = "https://github.com/$repo/"
    }
}