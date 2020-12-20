import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.4.21"
    id("com.github.ben-manes.versions") version "0.36.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
}

group = "failfast"
version = "0.1"

val coroutinesVersion = "1.4.2"

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    implementation(enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:1.4.21"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("io.strikt:strikt-core:0.28.1")
}

tasks {
    create<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
}

tasks.withType<JavaCompile>() {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

artifacts {
    add("archives", tasks["jar"])
    add("archives", tasks["sourceJar"])
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourceJar"])
            groupId = project.group as String
            artifactId = "failfast"
            version = project.version as String
        }
    }
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

val testMain = task("testMain", JavaExec::class) {
    main = "failfast.FailFastBootstrapTestKt"
    classpath = sourceSets["test"].runtimeClasspath
}

tasks.check {
    dependsOn(testMain)
}
