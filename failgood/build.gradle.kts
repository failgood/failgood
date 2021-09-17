import failgood.versions.coroutinesVersion
import failgood.versions.junitPlatformVersion
import failgood.versions.kotlinVersion
import failgood.versions.pitestVersion
import failgood.versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
    id("info.solidsoft.pitest")
    signing
    id("failgood.common")
}
// to release:
// ./gradlew publishToSonatype closeSonatypeStagingRepository (or closeAndReleaseSonatypeStagingRepository)


dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    api("org.junit.platform:junit-platform-commons:$junitPlatformVersion")
    // to enable running test in idea without having to add the dependency manually
    runtimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    compileOnly("org.junit.platform:junit-platform-engine:$junitPlatformVersion")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("org.pitest:pitest:$pitestVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.pitest:pitest:$pitestVersion")
    testImplementation("org.junit.platform:junit-platform-engine:$junitPlatformVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
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
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xuse-ir")
            languageVersion = "1.5"
            apiVersion = "1.5"
        }
    }
}


val testMain =
    task("testMain", JavaExec::class) {
        mainClass.set("failgood.FailGoodBootstrapKt")
        classpath = sourceSets["test"].runtimeClasspath
    }
val multiThreadedTest =
    task("multiThreadedTest", JavaExec::class) {
        mainClass.set("failgood.MultiThreadingPerformanceTestXKt")
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    mainClass.set("failgood.AutoTestMainKt")
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check { dependsOn(testMain, multiThreadedTest) }

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        //        verbose.set(true)
        jvmArgs.set(listOf("-Xmx512m")) // necessary on CI
        testPlugin.set("failgood")
        avoidCallsTo.set(setOf("kotlin.jvm.internal", "kotlin.Result"))
        targetClasses.set(setOf("failgood.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failgood.*Test", "failgood.**.*Test"))
        pitestVersion.set(failgood.versions.pitestVersion)
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}

