package failgood.experiments

import com.sun.jdi.Bootstrap
import com.sun.jdi.ClassType
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.VMDeathEvent
import failgood.Test
import failgood.describe

@Test
object DebuggerTest {
    val context = describe("experimenting with debugging") {
        test("whatever") {
            val clazzName = Debuggee::class.java.name
            println(clazzName)
            val launchingConnector = Bootstrap.virtualMachineManager()
                .defaultConnector()
            val arguments = launchingConnector.defaultArguments()
            arguments["main"]!!.setValue(clazzName)
            val vm = launchingConnector.launch(arguments)

            val classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest()
            classPrepareRequest.addClassFilter("*Debugee*")
            classPrepareRequest.enable()
            val queue = vm.eventQueue()
            try {
                while (true) {
                    val eventSet = queue.remove() ?: break
                    eventSet.forEach { ev ->
                        when (ev) {
                            is VMDeathEvent -> {
                                println("death:$ev")
                            }
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
