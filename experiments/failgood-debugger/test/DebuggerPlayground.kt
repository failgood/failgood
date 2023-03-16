package failgood.experiments

import com.sun.jdi.Bootstrap
import com.sun.jdi.ClassType
import com.sun.jdi.StackFrame
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import failgood.Test
import failgood.describe

@Test
object DebuggerPlayground {
    val context = describe("experimenting with debugging") {
        test("whatever") {
            val mainClass = Debuggee::class.java.name
            val classPath = System.getProperty("java.class.path")
            val launchingConnector = Bootstrap.virtualMachineManager()
                .defaultConnector()
            val arguments = launchingConnector.defaultArguments()
            arguments["main"]!!.setValue(mainClass)
            arguments["options"]!!.setValue("-cp \"$classPath\"")
            val vm = launchingConnector.launch(arguments)

            val eventRequestManager = vm.eventRequestManager()
            val classPrepareRequest = eventRequestManager.createClassPrepareRequest()
            classPrepareRequest.addClassFilter(mainClass)
            classPrepareRequest.enable()
            val queue = vm.eventQueue()
            try {
                while (true) {
                    val eventSet = queue.remove() ?: break
                    eventSet.forEach { ev ->
                        when (ev) {
                            is ClassPrepareEvent -> {
                                val classType = ev.referenceType() as ClassType
                                classType.allLineLocations()
                                    .forEach { eventRequestManager.createBreakpointRequest(it).also { it.enable() } }
                            }
                            is BreakpointEvent -> {
                                val event = ev
                                val stackFrame: StackFrame = event.thread().frame(0)

                                if (stackFrame.location().toString().contains(mainClass)) {
                                    val visibleVariables = stackFrame.getValues(stackFrame.visibleVariables())
                                    println("Variables at " + stackFrame.location().toString() + " > ")
                                    for ((key, value) in visibleVariables) {
                                        println(key.name() + " = " + value)
                                    }
                                }
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
