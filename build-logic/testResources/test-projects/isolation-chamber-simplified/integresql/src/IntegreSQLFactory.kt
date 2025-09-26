package com.test.isolationchamber.integresql

import com.test.isolationchamber.core.PostgresDb
import com.test.isolationchamber.core.PostgresqlFactory

class IntegreSQLFactory : PostgresqlFactory {
    override fun prepareDatabase(schema: String?): PostgresDb {
        return IntegreSQLDatabase("test-connection-string")
    }

    override fun cleanUp() {
        // Cleanup implementation
    }
}

class IntegreSQLDatabase(override val connectionString: String) : PostgresDb {
    override fun close() {
        // Close implementation
    }
}
