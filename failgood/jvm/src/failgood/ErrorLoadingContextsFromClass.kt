package failgood

internal class ErrorLoadingContextsFromClass(
    message: String,
    val jClass: Class<out Any>,
    override val cause: Throwable? = null
) : FailGoodException(message, cause)
