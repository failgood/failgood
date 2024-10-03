package failgood.internal.sysinfo

import failgood.internal.util.pluralize
import java.lang.management.ManagementFactory

private val operatingSystemMXBean =
    ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()

internal actual fun upt(): Long = runtimeMXBean.uptime

internal actual fun uptime(totalTests: Int?): String {
    val uptime = upt()
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "${uptime}ms. load:$percentage%." +
        if (totalTests != null) {
            " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
        } else ""
}

internal actual fun cpus() = Runtime.getRuntime().availableProcessors()

internal actual fun isRunningOnWindows() = System.getProperty("os.name").startsWith("Windows")
