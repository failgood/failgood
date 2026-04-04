import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins { id("buildgood.kmp") }

commonBuild {
    jvmTarget { production(JvmTarget.JVM_17) }
}

kotlin { jvm() }

tasks.register("printConfiguredTargets") {
    doLast {
        val mainCompile = tasks.named("compileKotlinJvm", KotlinJvmCompile::class.java).get()
        val testCompile = tasks.named("compileTestKotlinJvm", KotlinJvmCompile::class.java).get()
        println(
            "module=overridden mainJvmTarget=${mainCompile.compilerOptions.jvmTarget.orNull} testJvmTarget=${testCompile.compilerOptions.jvmTarget.orNull}"
        )
    }
}
