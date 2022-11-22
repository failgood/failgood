package failgood.junit.jupiter

import failgood.junit.execute
import failgood.junit.jupiter.fixtures.JunitTest
import org.junit.platform.engine.discovery.DiscoverySelectors

/*
  run a junit jupiter test and log what events it creates.
 */
suspend fun main() {
    execute(listOf(DiscoverySelectors.selectClass(JunitTest::class.java)))
}
