package failgood.experiments

import com.sun.jdi.Bootstrap
import com.sun.jdi.ClassType
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.ClassPrepareEvent
import failgood.Test
import failgood.describe

@Test
object DebuggerPlayground {
    val context = describe("experimenting with debugging") {
        test("whatever") {
            val mainClass = Debuggee::class.java.name
            println(mainClass)
            val classPath = System.getProperty("java.class.path")
            println(classPath)
            val launchingConnector = Bootstrap.virtualMachineManager()
                .defaultConnector()
            val arguments = launchingConnector.defaultArguments()
            arguments["main"]!!.setValue(mainClass)
            arguments["options"]!!.setValue("-cp \"$classPath\"")
            val vm = launchingConnector.launch(arguments)

            val classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest()
            classPrepareRequest.addClassFilter(mainClass)
            classPrepareRequest.enable()
            val queue = vm.eventQueue()
            try {
                while (true) {
                    val eventSet = queue.remove() ?: break
                    eventSet.forEach { ev ->
                        when (ev) {
                            is ClassPrepareEvent -> {
                                println("r:" + ev.referenceType())
                                val classType = ev.referenceType() as ClassType
                                println(classType.allLineLocations().map { it.lineNumber() })
                            }
                        }
                    }
                    vm.resume()
                }
            } catch (e: VMDisconnectedException) {
                println("vm disconnected")
            }
        }
    }
}

object Debuggee {
    @JvmStatic
    fun main(args: Array<String>) {
        val name = "blubbi"
        val name2 = "boring"

        println("$name $name2")
    }
}
