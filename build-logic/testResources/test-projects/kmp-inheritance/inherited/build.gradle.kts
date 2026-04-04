import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins { id("buildgood.kmp") }

kotlin { jvm() }

tasks.register("printConfiguredTargets") {
    doLast {
        val mainCompile = tasks.named("compileKotlinJvm", KotlinJvmCompile::class.java).get()
        val testCompile = tasks.named("compileTestKotlinJvm", KotlinJvmCompile::class.java).get()
        println(
            "module=inherited mainJvmTarget=${mainCompile.compilerOptions.jvmTarget.orNull} testJvmTarget=${testCompile.compilerOptions.jvmTarget.orNull}"
        )
    }
}
