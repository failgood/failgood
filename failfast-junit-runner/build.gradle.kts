import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    `maven-publish`
    id("com.jfrog.bintray")

}


repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    api(project(":failfast"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.junit.platform:junit-platform-engine:1.7.0")
    testImplementation("org.junit.platform:junit-platform-launcher:1.7.0")
}

tasks {
    create<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
    withType<Test> { enabled = false }
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }
}

val testMain =
    task("testMain", JavaExec::class) {
        main = "failfast.junit.AllTestsKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    main = "failfast.junit.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check { dependsOn(testMain) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        mutators.set(listOf("ALL"))
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failfast")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failfast.junit.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failfast.junit.*Test", "failfast.junit.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("Fail Fast")
                description.set("a fast test runner for kotlin")
                url.set("https://github.com/christophsturm/failfast")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
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
                    connection.set("scm:git:https://github.com/christophsturm/failfast.git")
                    developerConnection.set("scm:git:git@github.com:christophsturm/failfast.git")
                    url.set("https://github.com/christophsturm/failfast/")
                }
            }
            from(components["java"])
            artifact(tasks["sourceJar"])
            groupId = project.group as String
            artifactId = "failfast-junit"
            version = project.version as String
        }
    }
}
// BINTRAY_API_KEY= ... ./gradlew clean build publish bintrayUpload
/*
bintray {
    user = "christophsturm"
    key = System.getenv("BINTRAY_API_KEY")
    publish = true
    setPublications("mavenJava")
    pkg(
        delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
            repo = "maven"
            name = "failfast"
            setLicenses("MIT")
            version(
                delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
                    name = project.version as String
                }
            )
        }
    )
}
*/

