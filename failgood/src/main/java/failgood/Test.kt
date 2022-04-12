package failgood

import org.junit.platform.commons.annotation.Testable

/**
 * This annotation tells the junit platform that a class or object is a failgood test.
 */
@Testable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Test
