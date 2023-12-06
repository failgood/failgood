package failgood.examples

import mu.KLogger
import mu.KotlinLogging

class LoggingComponent(private val logger: KLogger = KotlinLogging.logger {}) {
    fun functionThatLogs() {
        logger.debug { "I logged this" }
    }
}
