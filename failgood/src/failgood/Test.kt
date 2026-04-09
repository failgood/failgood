package failgood

import org.junit.platform.commons.annotation.Testable

/** Indicate that something contains failgood tests. Works on object, class, function or files. */
@Testable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class Test
