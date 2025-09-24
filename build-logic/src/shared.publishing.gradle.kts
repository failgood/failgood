import buildlogic.PublishingBuildExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

plugins {
    java
    `maven-publish`
    signing
}

// Create the DSL extension
val publishingBuild = extensions.create<PublishingBuildExtension>("publishingBuild")

// Configure from gradle.properties
publishingBuild.apply {
    projectInfo {
        name = findProperty("project.name") as String? ?: project.name
        description = findProperty("project.description") as String?
        url = findProperty("project.url") as String?
    }
    scm {
        val repo = findProperty("project.repo") as String?
        if (repo != null) {
            fromGitHub(repo)
        }
    }
}

// Apply configuration after evaluation
afterEvaluate {
    val pub = "maven"

    publishing {
        repositories {
            maven {
                setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.properties["ossrhUsername"] as String?
                    password = project.properties["ossrhPassword"] as String?
                }
            }
        }

        publications {
            create<MavenPublication>(pub) {
                from(components["java"])

                pom {
                    // Use configured values from DSL - projects will set these
                    name.set(publishingBuild.projectInfo.name ?: project.name)
                    publishingBuild.projectInfo.description?.let { description.set(it) }
                    publishingBuild.projectInfo.url?.let { url.set(it) }

                    licenses {
                        license {
                            name.set("The MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("christophsturm")
                            name.set("Christoph Sturm")
                            email.set("me@christophsturm.com")
                        }
                    }

                    scm {
                        publishingBuild.scm.connection?.let { connection.set(it) }
                        publishingBuild.scm.developerConnection?.let { developerConnection.set(it) }
                        publishingBuild.scm.url?.let { url.set(it) }
                    }
                }
            }
        }
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    signing {
        sign(publishing.publications[pub])
    }
}

tasks {
    register<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
}