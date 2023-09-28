package failgood.internal

import failgood.internal.util.pluralize
import java.lang.management.ManagementFactory

actual object SysInfo {
    private val operatingSystemMXBean =
        ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    private val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
    internal actual fun upt(): Long = runtimeMXBean.uptime

    internal actual fun uptime(totalTests: Int?): String {
        val uptime = upt()
        val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
        val percentage = cpuTime * 100 / uptime
        return "${uptime}ms. load:$percentage%." + if (totalTests != null) {
            " " + pluralize(totalTests * 1000 / uptime.toInt(), "test") + "/sec"
        } else
            ""
    }
    actual fun cpus() = Runtime.getRuntime().availableProcessors()
}
