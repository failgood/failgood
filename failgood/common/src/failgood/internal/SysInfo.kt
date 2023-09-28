package failgood.internal

expect object SysInfo {
    internal fun upt(): Long
    internal fun uptime(totalTests: Int? = null): String
    fun cpus(): Int
}
