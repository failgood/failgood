import com.jfrog.bintray.gradle.BintrayExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.jfrog.bintray")
    id("info.solidsoft.pitest")
}

val coroutinesVersion = "1.4.2"


repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.4.21-2"))
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:0.28.1")
    testImplementation("org.pitest:pitest:1.6.2")
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

artifacts {
    add("archives", tasks["jar"])
    add("archives", tasks["sourceJar"])
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
            artifactId = "failfast"
            version = project.version as String
        }
    }
}
// BINTRAY_API_KEY= ... ./gradlew clean build publish bintrayUpload
bintray {
    user = "christophsturm"
    key = System.getenv("BINTRAY_API_KEY")
    publish = true
    setPublications("mavenJava")
    pkg(
        delegateClosureOf<BintrayExtension.PackageConfig> {
            repo = "maven"
            name = "failfast"
            setLicenses("MIT")
            version(
                delegateClosureOf<BintrayExtension.VersionConfig> {
                    name = project.version as String
                }
            )
        }
    )
}


val testMain =
    task("testMain", JavaExec::class) {
        main = "failfast.FailFastBootstrapKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
val multiThreadedTest =
    task("multiThreadedTest", JavaExec::class) {
        main = "failfast.MultiThreadingPerformanceTestXKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    main = "failfast.AutoTestMainKt"
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check { dependsOn(testMain, multiThreadedTest) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failfast")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failfast.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failfast.*Test", "failfast.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

