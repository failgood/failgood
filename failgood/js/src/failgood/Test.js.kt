package failgood

/**
 * Indicate that something contains failgood tests. Works on object, class, function or files.
 */
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class Test
