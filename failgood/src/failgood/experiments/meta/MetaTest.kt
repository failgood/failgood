package failgood.experiments.meta

import org.junit.platform.commons.annotation.Testable

@Testable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class MetaTest
