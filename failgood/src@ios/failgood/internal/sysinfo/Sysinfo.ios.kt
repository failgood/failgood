package failgood.internal.sysinfo

import kotlin.math.roundToLong
import platform.Foundation.NSProcessInfo

internal actual fun upt(): Long = (NSProcessInfo.processInfo.systemUptime * 1000).roundToLong()

internal actual fun uptime(totalTests: Int?): String = "${upt()}ms."

internal actual fun cpus(): Int = NSProcessInfo.processInfo.processorCount.toInt()

internal actual fun isRunningOnWindows(): Boolean = false
