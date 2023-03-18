package failgood.experiments

import com.sun.jdi.Bootstrap
import com.sun.jdi.ClassType
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import failgood.Test
import failgood.describe
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.assertNotNull

@Test
object DebuggerPlayground {
    val context = describe("experimenting with debugging") {
        test("can start a class in a new vm and get variable values for every line") {
            val mainClass = Debuggee::class.java.name
            val variableInfo = runClass(mainClass)
            assertNotNull(variableInfo[10]).let {
                assertEquals(it["name"], "\"blubbi\"")
            }
        }
    }

    private fun runClass(mainClass: String): MutableMap<Int, Map<String, String>> {
        val results = mutableMapOf<Int, Map<String, String>>()
        val classPath = System.getProperty("java.class.path")
        val launchingConnector = Bootstrap.virtualMachineManager()
            .defaultConnector()
        val arguments = launchingConnector.defaultArguments()
        arguments["main"]!!.setValue("$mainClass firstArg secondArg")
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
                            classType.allLineLocations() // todo filter duplicate line numbers
                                .forEach { location ->
                                    eventRequestManager.createBreakpointRequest(location).enable()
                                }
                        }

                        is BreakpointEvent -> {
                            val frame = ev.thread().frame(0)

                            val location = frame.location()
                            val locationName = location.toString()
                            if (locationName.contains(mainClass)) {
                                val vars = frame.getValues(frame.visibleVariables())
                                val lineResults =
                                    vars.entries.associate { (key, value) -> key.name() to value.toString() }
                                results[location.lineNumber()] = lineResults
                            }
                        }
                    }
                }
                vm.resume()
            }
        } catch (e: VMDisconnectedException) {
            println("vm disconnected")
        }
        return results
    }
}
