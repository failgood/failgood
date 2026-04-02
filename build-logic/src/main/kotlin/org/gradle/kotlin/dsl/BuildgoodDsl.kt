package org.gradle.kotlin.dsl

import buildgood.PublishingBuildExtension
import org.gradle.api.Project

fun Project.publish(action: PublishingBuildExtension.() -> Unit = {}) {
    extensions.getByType(PublishingBuildExtension::class.java).apply {
        enabled = true
        action()
    }
}
