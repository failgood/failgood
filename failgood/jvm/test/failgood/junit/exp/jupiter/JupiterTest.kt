package failgood.junit.exp.jupiter

import failgood.junit.exp.events.execute
import failgood.junit.exp.jupiter.fixtures.JunitTest
import org.junit.platform.engine.discovery.DiscoverySelectors

/*
  run a junit jupiter test and log what events it creates.
 */
suspend fun main() {
    execute(listOf(DiscoverySelectors.selectClass(JunitTest::class.java)))
}
