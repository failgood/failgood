package failgood.internal.sysinfo

internal expect fun upt(): Long

internal expect fun uptime(totalTests: Int? = null): String

internal expect fun cpus(): Int

internal expect fun isRunningOnWindows(): Boolean
