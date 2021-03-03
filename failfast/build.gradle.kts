import failfast.Versions.coroutinesVersion
import failfast.Versions.junitPlatformVersion
import failfast.Versions.kotlinVersion
import failfast.Versions.pitestVersion
import failfast.Versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.jfrog.bintray")
    id("info.solidsoft.pitest")
    signing
    id("failfast.common")
}



repositories {
    mavenCentral()
}
dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:$pitestVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.pitest:pitest:$pitestVersion")
    compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testImplementation("com.christophsturm:filepeek:0.1.3")
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
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
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

