package failgood.internal.util

actual fun getenv(name: String): String? {
    return System.getenv(name)
}
