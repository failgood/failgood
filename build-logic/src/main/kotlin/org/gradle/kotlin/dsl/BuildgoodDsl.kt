package org.gradle.kotlin.dsl

import buildgood.PitestBuildExtension
import buildgood.PublishingBuildExtension
import org.gradle.api.Project

fun Project.pitest(action: PitestBuildExtension.() -> Unit = {}) {
    extensions.getByType(PitestBuildExtension::class.java).apply(action)
}

fun Project.publish(action: PublishingBuildExtension.() -> Unit = {}) {
    extensions.getByType(PublishingBuildExtension::class.java).apply {
        enabled = true
        action()
    }
}
