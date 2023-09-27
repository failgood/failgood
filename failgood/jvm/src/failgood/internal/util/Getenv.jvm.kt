package failgood.internal.util

internal actual fun getEnv(name: String): String? = System.getenv(name)
