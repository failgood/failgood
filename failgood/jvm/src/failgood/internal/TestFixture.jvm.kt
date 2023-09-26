package failgood.internal

import org.junit.platform.commons.annotation.Testable

@Testable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class TestFixture
