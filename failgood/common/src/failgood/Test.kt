package failgood

/**
 * Indicate that something contains failgood tests. Works on object, class, function or files.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class Test
