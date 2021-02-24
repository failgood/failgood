import failfast.Versions.coroutinesVersion
import failfast.Versions.striktVersion
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("info.solidsoft.pitest")
    id("com.jfrog.bintray")
    id("failfast.common")

}


repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    api(project(":failfast"))
    api("io.r2dbc:r2dbc-spi:0.8.3.RELEASE")
    testImplementation("io.strikt:strikt-core:$striktVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:$coroutinesVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:${failfast.Versions.junitPlatformVersion}")

    runtimeOnly("io.r2dbc:r2dbc-h2:0.8.4.RELEASE")
    runtimeOnly("com.h2database:h2:1.4.200")
    runtimeOnly("org.postgresql:postgresql:42.2.19")
    runtimeOnly("io.r2dbc:r2dbc-postgresql:0.8.6.RELEASE")
    runtimeOnly("io.r2dbc:r2dbc-pool:0.8.5.RELEASE")
    implementation("org.testcontainers:postgresql:1.15.2")
    implementation("org.flywaydb:flyway-core:7.5.3")

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
        main = "failfast.r2dbc.AllTestsKt"
        classpath = sourceSets["test"].runtimeClasspath
    }
task("autotest", JavaExec::class) {
    main = "failfast.r2dbc.AutoTestMainKt"
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
        targetClasses.set(setOf("failfast.r2dbc.*")) //by default "${project.group}.*"
        targetTests.set(setOf("failfast.r2dbc.*Test", "failfast.r2dbc.**.*Test"))
        pitestVersion.set("1.6.2")
        threads.set(
            System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        )
        outputFormats.set(setOf("XML", "HTML"))
    }
}
