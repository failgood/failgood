import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.4.21"
    id("com.github.ben-manes.versions") version "0.36.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
}

group = "nanotest"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}
dependencies {
    testImplementation("io.strikt:strikt-core:0.28.1")
}

tasks {
    create<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
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
            artifactId = "nanotest"
            version = project.version as String
        }
    }
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }
