package failgood.experiments

object Debuggee {
    @JvmStatic
    fun main(args: Array<String>) {
        val name = "blubbi"
        val name2 = "boring"
        val name3 = (name + name2).uppercase()

        println("$name $name2 $name3")
    }
}
