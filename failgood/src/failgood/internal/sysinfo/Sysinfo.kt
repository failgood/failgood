package failgood.internal.sysinfo

import failgood.internal.util.pluralize
import java.lang.management.ManagementFactory

private val operatingSystemMXBean =
    ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()

internal fun upt(): Long = runtimeMXBean.uptime

internal fun uptime(totalTests: Int? = null): String {
    val uptime = upt()
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "${uptime}ms. load:$percentage%." +
        if (totalTests != null) {
            " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
        } else ""
}

internal fun cpus() = Runtime.getRuntime().availableProcessors()

internal fun isRunningOnWindows() = System.getProperty("os.name").startsWith("Windows")
