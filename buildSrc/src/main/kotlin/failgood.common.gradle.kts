import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    `maven-publish`
    signing
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    ktlint("com.pinterest:ktlint:0.43.2")
}

val pub = "mavenJava-${project.name}"
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
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
            pom {
                name.set("FailGood")
                description.set("a fast test runner for kotlin")
                url.set("https://github.com/failgood/failgood")
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
                    connection.set("scm:git:https://github.com/failgood/failgood.git")
                    developerConnection.set("scm:git:git@github.com:failgood/failgood.git")
                    url.set("https://github.com/failgood/failgood/")
                }
            }
        }
    }
}
java {
    @Suppress("UnstableApiUsage")
    withJavadocJar()
    @Suppress("UnstableApiUsage")
    withSourcesJar()
}

signing {
    sign(publishing.publications[pub])
}
tasks {
    create<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
    withType<Test> {
        useJUnitPlatform()
        outputs.upToDateWhen { false }
    }

    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xuse-ir")
            languageVersion = "1.6"
            apiVersion = "1.6"
        }
    }
}
